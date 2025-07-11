package no.nav.foreldrepenger.oversikt.domene.svp;

import static java.util.function.Predicate.not;
import static no.nav.foreldrepenger.oversikt.domene.svp.AvslutningUtleder.utledDato;
import static no.nav.foreldrepenger.oversikt.domene.svp.AvslutningUtleder.utledÅrsak;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.common.innsyn.Arbeidstidprosent;
import no.nav.foreldrepenger.common.innsyn.svp.PeriodeResultat;
import no.nav.foreldrepenger.common.innsyn.svp.SvpArbeidsforhold;
import no.nav.foreldrepenger.common.innsyn.svp.SvpSak;
import no.nav.foreldrepenger.common.innsyn.svp.SvpÅpenBehandling;
import no.nav.foreldrepenger.common.innsyn.svp.Søknad;
import no.nav.foreldrepenger.common.innsyn.svp.TilretteleggingType;
import no.nav.foreldrepenger.common.innsyn.svp.Vedtak;
import no.nav.foreldrepenger.oversikt.domene.BehandlingTilstandUtleder;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

final class DtoMapper {

    private DtoMapper() {
    }

    static SvpSak mapFra(SakSVP0 sak) {
        var gjeldendeVedtak = gjeldendeVedtak(sak)
            .map(DtoMapper::tilDto)
            .orElse(null);
        var åpenBehandling = sak.søknadUnderBehandling().map(s -> {
            var behandlingTilstand = BehandlingTilstandUtleder.utled(sak.aksjonspunkt());
            var avslutningDato = sak.familieHendelse() == null ? null : utledDato(sak.familieHendelse());
            var søknadDto = tilDto(s, avslutningDato);
            return new SvpÅpenBehandling(behandlingTilstand, søknadDto);
        }).orElse(null);
        var familiehendelse = sak.familieHendelse() == null ? null : sak.familieHendelse().tilDto();
        return new SvpSak(sak.saksnummer().tilDto(), familiehendelse, sak.avsluttet(), åpenBehandling, gjeldendeVedtak, sak.oppdatertTidspunkt());
    }

    private static Vedtak tilDto(SvpVedtak vedtak) {
        var a = vedtak.arbeidsforhold().stream().map(arbeidsforhold -> {
            var tilrettelegginger = arbeidsforhold.svpPerioder()
                .stream()
                .filter(not(p -> p.resultatÅrsak().erOpphør()))
                .filter(not(p -> p.resultatÅrsak().equals(ResultatÅrsak.AVSLAG_INNGANGSVILKÅR)))
                .map(DtoMapper::tilDto)
                .collect(Collectors.toSet());
            var avslutningÅrsak = utledÅrsak(arbeidsforhold.ikkeOppfyltÅrsak(), arbeidsforhold.svpPerioder());
            var oppholdsperioder = arbeidsforhold.oppholdsperioder().stream().map(DtoMapper::tilDto).collect(Collectors.toSet());
            var tilretteleggingJustertForOpphold = fjernPerioderMedOpphold(tilrettelegginger, oppholdsperioder);
            return new SvpArbeidsforhold(arbeidsforhold.aktivitet().tilDto(), arbeidsforhold.behovFom(), arbeidsforhold.risikoFaktorer(),
                arbeidsforhold.tiltak(), tilretteleggingJustertForOpphold, oppholdsperioder, avslutningÅrsak);
        }).collect(Collectors.toSet());
        var avslagÅrsak = vedtak.avslagÅrsak() == null ? null : switch (vedtak.avslagÅrsak()) {
            case ARBEIDSGIVER_KAN_TILRETTELEGGE -> Vedtak.AvslagÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE;
            case SØKER_ER_INNVILGET_SYKEPENGER -> Vedtak.AvslagÅrsak.SØKER_ER_INNVILGET_SYKEPENGER;
            case MANGLENDE_DOKUMENTASJON -> Vedtak.AvslagÅrsak.MANGLENDE_DOKUMENTASJON;
            case ANNET -> Vedtak.AvslagÅrsak.ANNET;
        };
        return new Vedtak(a, avslagÅrsak);
    }


    private static no.nav.foreldrepenger.common.innsyn.svp.Tilrettelegging tilDto(SvpPeriode periode) {
        var resultatType = switch (periode.resultatÅrsak()) {
            case INNVILGET -> PeriodeResultat.ResultatType.INNVILGET;
            case AVSLAG_SØKNADSFRIST -> PeriodeResultat.ResultatType.AVSLAG_SØKNADSFRIST;
            case AVSLAG_ANNET -> PeriodeResultat.ResultatType.AVSLAG_ANNET;
            default -> throw new IllegalStateException("Unexpected value: " + periode.resultatÅrsak());
        };
        var resultat = new PeriodeResultat(resultatType, new PeriodeResultat.Utbetalingsgrad(periode.utbetalingsgrad().decimalValue()));
        var tilretteleggingType = switch (periode.tilretteleggingType()) {
            case HEL -> TilretteleggingType.HEL;
            case DELVIS -> TilretteleggingType.DELVIS;
            case INGEN -> TilretteleggingType.INGEN;
        };
        var arbeidstidprosent = periode.arbeidstidprosent() == null ? null : new Arbeidstidprosent(periode.arbeidstidprosent().decimalValue());
        return new no.nav.foreldrepenger.common.innsyn.svp.Tilrettelegging(periode.fom(), periode.tom(), tilretteleggingType, arbeidstidprosent, resultat);
    }

    private static Søknad tilDto(SvpSøknad søknad, LocalDate avslutningDato) {
        var arbeidsforhold = søknad.tilrettelegginger().stream().map(t -> tilDto(t, avslutningDato)).collect(Collectors.toSet());
        return new Søknad(arbeidsforhold);
    }

    private static SvpArbeidsforhold tilDto(Tilrettelegging tilrettelegging, LocalDate avslutningDato) {
        var oppholdDtos = tilrettelegging.oppholdsperioder().stream().map(DtoMapper::tilDto).collect(Collectors.toSet());
        var tilretteleggingDtos = tilrettelegging.perioder().stream().map(p -> tilDto(p, utledTom(p, tilrettelegging.perioder(), avslutningDato)))
            .collect(Collectors.toSet());
        var tilrettelegginger = fjernPerioderMedOpphold(tilretteleggingDtos, oppholdDtos);
        return new SvpArbeidsforhold(tilrettelegging.aktivitet().tilDto(), tilrettelegging.behovFom(),
            tilrettelegging.risikoFaktorer(), tilrettelegging.tiltak(), tilrettelegginger, oppholdDtos, null);
    }

    private static Set<no.nav.foreldrepenger.common.innsyn.svp.Tilrettelegging> fjernPerioderMedOpphold(Set<no.nav.foreldrepenger.common.innsyn.svp.Tilrettelegging> tilretteleggingDtos,
                                                                                                        Set<no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode> oppholdDtos) {
        var timelineOpphold = oppholdDtos.stream()
            .map(p -> new LocalDateSegment<>(p.fom(), p.tom(), Boolean.TRUE))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, StandardCombinators::alwaysTrueForMatch)));
        var timelineTilrettelegginger = tilretteleggingDtos.stream()
            .map(p -> new LocalDateSegment<>(p.fom(), p.tom(), p))
            .collect(Collectors.collectingAndThen(Collectors.toList(), LocalDateTimeline::new));
        return timelineTilrettelegginger.disjoint(timelineOpphold, (datoInterval, tl, opphold) -> {
            var t = new no.nav.foreldrepenger.common.innsyn.svp.Tilrettelegging(datoInterval.getFomDato(), datoInterval.getTomDato(),
                tl.getValue().type(), tl.getValue().arbeidstidprosent(), tl.getValue().resultat());
            return new LocalDateSegment<>(datoInterval, t);
        }).stream().map(LocalDateSegment::getValue).collect(Collectors.toSet());
    }

    private static LocalDate utledTom(TilretteleggingPeriode periode, Set<TilretteleggingPeriode> allePerioder, LocalDate avslutningDato) {
        return allePerioder.stream().map(TilretteleggingPeriode::fom).sorted()
            .filter(fom -> fom.isAfter(periode.fom()))
            .findFirst()
            .map(fom -> fom.minusDays(1))
            .orElseGet(() -> avslutningDato.isBefore(periode.fom()) ? periode.fom() : avslutningDato);
    }

    private static no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode tilDto(OppholdPeriode opphold) {
        return new no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode(opphold.fom(), opphold.tom(),
            switch (opphold.årsak()) {
            case FERIE -> no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode.OppholdÅrsak.FERIE;
            case SYKEPENGER -> no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode.OppholdÅrsak.SYKEPENGER;
            },
            switch (opphold.kilde()) {
            case SAKSBEHANDLER -> no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode.OppholdKilde.SAKSBEHANDLER;
            case INNTEKTSMELDING -> no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode.OppholdKilde.INNTEKTSMELDING;
            case SØKNAD -> no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode.OppholdKilde.SØKNAD;
            });
    }

    private static no.nav.foreldrepenger.common.innsyn.svp.Tilrettelegging tilDto(TilretteleggingPeriode tilretteleggingPeriode, LocalDate tom) {
        var type = switch (tilretteleggingPeriode.type()) {
            case HEL -> TilretteleggingType.HEL;
            case DELVIS -> TilretteleggingType.DELVIS;
            case INGEN -> TilretteleggingType.INGEN;
        };
        var arbeidstidprosent = tilretteleggingPeriode.arbeidstidprosent() == null ? null : new Arbeidstidprosent(tilretteleggingPeriode.arbeidstidprosent().decimalValue());
        return new no.nav.foreldrepenger.common.innsyn.svp.Tilrettelegging(tilretteleggingPeriode.fom(), tom, type, arbeidstidprosent,
            null);
    }

    private static Optional<SvpVedtak> gjeldendeVedtak(SakSVP0 sak) {
        return sak.vedtak().stream().max(Comparator.comparing(SvpVedtak::vedtakstidspunkt));
    }
}
