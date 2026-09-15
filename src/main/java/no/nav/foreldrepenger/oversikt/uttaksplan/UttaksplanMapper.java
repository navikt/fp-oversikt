package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.Aktivitet;
import static no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.Arbeidstidprosent;
import static no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.EøsUttakDto;
import static no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.Gradering;
import static no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.Rolle;
import static no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.SamtidigUttak;
import static no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.UttakDto;
import static no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.VedtattResultat;
import static no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.OverføringÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.UtsettelseÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.VirkedagJusterer;

final class UttaksplanMapper {

    private UttaksplanMapper() {
    }

    static UttakDto mapUttak(Uttaksperiode periode, BrukerRolle brukerRolle) {
        var rolle = mapRolle(brukerRolle);
        var kontoType = periode.utledKontoType().map(Konto::tilDto).orElse(null);
        var utsettelseÅrsak = mapUtsettelseÅrsak(periode.utsettelseÅrsak());
        var overføringÅrsak = mapOverføringÅrsak(periode.overføringÅrsak());
        var morsAktivitet = periode.morsAktivitet() == null ? null : periode.morsAktivitet().tilDto();
        var samtidigUttak = periode.samtidigUttak() == null || !periode.samtidigUttak().merEnn0() ? null
            : new SamtidigUttak(periode.samtidigUttak().decimalValue());
        var gradering = utledGradering(periode).orElse(null);
        var resultat = mapResultat(periode.resultat());
        return new UttakDto(rolle, kontoType, utsettelseÅrsak, overføringÅrsak, gradering, morsAktivitet, samtidigUttak,
            periode.flerbarnsdager() != null && periode.flerbarnsdager(), resultat);
    }

    static UttakDto mapUttak(FpSøknadsperiode periode, BrukerRolle brukerRolle) {
        var samtidigUttak =
            periode.samtidigUttak() == null || !periode.samtidigUttak().merEnn0() ? null : new SamtidigUttak(periode.samtidigUttak().decimalValue());
        var gradering =
            samtidigUttak == null && periode.gradering() != null ? new Gradering(new Arbeidstidprosent(periode.gradering().prosent().decimalValue()),
                mapAktivitet(periode.gradering().uttakAktivitet())) : null;
        return new UttakDto(mapRolle(brukerRolle), periode.konto() == null ? null : periode.konto().tilDto(),
            mapUtsettelseÅrsak(periode.utsettelseÅrsak()), mapOverføringÅrsak(periode.overføringÅrsak()), gradering,
            periode.morsAktivitet() == null ? null : periode.morsAktivitet().tilDto(), samtidigUttak,
            periode.flerbarnsdager() != null && periode.flerbarnsdager(), null);
    }

    static List<Planperiode> mapVedtaksperioder(Collection<Uttaksperiode> perioder, BrukerRolle brukerRolle) {
        return Stream.ofNullable(perioder)
            .flatMap(Collection::stream)
            .filter(UttaksplanMapper::harBetydningForPlanen)
            .map(periode -> justerPlanperiode(periode.fom(), periode.tom(), mapUttak(periode, brukerRolle)))
            .flatMap(Optional::stream)
            .toList();
    }

    static List<Planperiode> mapSøknadsperioder(Collection<FpSøknadsperiode> perioder, BrukerRolle brukerRolle) {
        return Stream.ofNullable(perioder)
            .flatMap(Collection::stream)
            .map(periode -> justerPlanperiode(periode.fom(), periode.tom(), mapUttak(periode, brukerRolle)))
            .flatMap(Optional::stream)
            .toList();
    }

    private static Optional<Planperiode> justerPlanperiode(LocalDate fom, LocalDate tom, UttakDto uttak) {
        var justertFom = VirkedagJusterer.justerFom(fom);
        var justertTom = VirkedagJusterer.justerTom(tom);
        return justertFom.isAfter(justertTom) ? Optional.empty() : Optional.of(new Planperiode(justertFom, justertTom, uttak));
    }

    static EøsUttakDto mapEøsUttak(UttakPeriodeAnnenpartEøs periode) {
        return new EøsUttakDto(periode.kontoType().tilDto(),
            new EøsUttakDto.Trekkdager(periode.trekkdager()));
    }

    static FellesUttaksplanDto.Dekningsgrad mapDekningsgrad(Dekningsgrad dekningsgrad) {
        if (dekningsgrad == null) {
            return null;
        }
        return switch (dekningsgrad) {
            case ÅTTI -> FellesUttaksplanDto.Dekningsgrad.ÅTTI;
            case HUNDRE -> FellesUttaksplanDto.Dekningsgrad.HUNDRE;
        };
    }

    private static Optional<Gradering> utledGradering(Uttaksperiode periode) {
        if (periode.resultat() != null && Uttaksperiode.Resultat.Type.INNVILGET.equals(periode.resultat().type())) {
            return Optional.empty();
        }
        return aktiviteter(periode).max(Comparator.comparing(Uttaksperiode.UttaksperiodeAktivitet::arbeidstidsprosent))
            .filter(aktivitet -> aktivitet.arbeidstidsprosent().merEnn0())
            .map(aktivitet -> new Gradering(
                new Arbeidstidprosent(aktivitet.arbeidstidsprosent().decimalValue()), mapAktivitet(aktivitet.aktivitet())));
    }

    private static Aktivitet mapAktivitet(UttakAktivitet aktivitet) {
        if (aktivitet == null) {
            return null;
        }
        var arbeidsgiver = aktivitet.arbeidsgiver() == null ? null : mapArbeidsgiver(aktivitet.arbeidsgiver());
        return new Aktivitet(mapAktivitetType(aktivitet.type()), arbeidsgiver, null);
    }

    private static FellesUttaksplanDto.Arbeidsgiver mapArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        var dto = arbeidsgiver.tilDto();
        var type = switch (dto.type()) {
            case ORGANISASJON -> FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.ORGANISASJON;
            case PRIVAT -> FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.PRIVAT;
        };
        return new FellesUttaksplanDto.Arbeidsgiver(dto.id(), type);
    }

    private static Aktivitet.AktivitetType mapAktivitetType(UttakAktivitet.Type type) {
        return switch (type) {
            case ORDINÆRT_ARBEID -> Aktivitet.AktivitetType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Aktivitet.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> Aktivitet.AktivitetType.FRILANS;
            case ANNET -> Aktivitet.AktivitetType.ANNET;
        };
    }

    private static FellesUttaksplanDto.UtsettelseÅrsak mapUtsettelseÅrsak(UtsettelseÅrsak årsak) {
        if (årsak == null) {
            return null;
        }
        return switch (årsak) {
            case HV_ØVELSE -> FellesUttaksplanDto.UtsettelseÅrsak.HV_ØVELSE;
            case ARBEID -> FellesUttaksplanDto.UtsettelseÅrsak.ARBEID;
            case LOVBESTEMT_FERIE -> FellesUttaksplanDto.UtsettelseÅrsak.FERIE;
            case SØKER_SYKDOM -> FellesUttaksplanDto.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> FellesUttaksplanDto.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> FellesUttaksplanDto.UtsettelseÅrsak.BARN_INNLAGT;
            case NAV_TILTAK -> FellesUttaksplanDto.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> FellesUttaksplanDto.UtsettelseÅrsak.FRI;
        };
    }

    private static FellesUttaksplanDto.OverføringÅrsak mapOverføringÅrsak(OverføringÅrsak årsak) {
        if (årsak == null) {
            return null;
        }
        return switch (årsak) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> FellesUttaksplanDto.OverføringÅrsak.ALENEOMSORG;
        };
    }

    private static VedtattResultat mapResultat(Uttaksperiode.Resultat resultat) {
        if (resultat == null) {
            return null;
        }
        var trekkerDager = Stream.ofNullable(resultat.aktiviteter())
            .flatMap(Collection::stream)
            .anyMatch(aktivitet -> aktivitet.trekkdager().merEnn0());
        return new VedtattResultat(resultat.innvilget(), resultat.trekkerMinsterett(), trekkerDager,
            mapÅrsak(resultat.årsak()));
    }

    private static VedtattResultat.Årsak mapÅrsak(Uttaksperiode.Resultat.Årsak årsak) {
        if (årsak == null) {
            return VedtattResultat.Årsak.ANNET;
        }
        return switch (årsak) {
            case AVSLAG_FRATREKK_PLEIEPENGER -> VedtattResultat.Årsak.AVSLAG_FRATREKK_PLEIEPENGER;
            case INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID -> VedtattResultat.Årsak.INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID;
            case ANNET, AVSLAG_HULL_I_UTTAKSPLAN, AVSLAG_UTSETTELSE_TILBAKE_I_TID -> VedtattResultat.Årsak.ANNET;
        };
    }

    private static Rolle mapRolle(BrukerRolle brukerRolle) {
        return switch (brukerRolle) {
            case MOR -> Rolle.MOR;
            case FAR, MEDMOR -> Rolle.FAR_MEDMOR;
            case UKJENT -> throw new IllegalStateException("Ukjent brukerRolle burde ikke oppstå");
        };
    }

    private static Stream<Uttaksperiode.UttaksperiodeAktivitet> aktiviteter(Uttaksperiode periode) {
        return periode.resultat() == null ? Stream.empty() : Stream.ofNullable(periode.resultat().aktiviteter()).flatMap(Collection::stream);
    }

    private static boolean harBetydningForPlanen(Uttaksperiode periode) {
        if (periode.resultat() == null || periode.resultat().innvilget()) {
            return true;
        }
        var aktiviteter = aktiviteter(periode).toList();
        return aktiviteter.isEmpty() || aktiviteter.stream().anyMatch(aktivitet -> aktivitet.trekkdager().merEnn0());
    }
}
