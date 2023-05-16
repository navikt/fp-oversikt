package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.domene.BrukerRolle.MOR;
import static no.nav.foreldrepenger.oversikt.domene.Konto.FORELDREPENGER;
import static no.nav.foreldrepenger.oversikt.domene.SakStatus.avsluttet;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.FpÅpenBehandling;
import no.nav.foreldrepenger.common.innsyn.Person;
import no.nav.foreldrepenger.common.innsyn.RettighetType;
import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;


public record SakFP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId,
                     @JsonProperty("status") SakStatus status,
                     @JsonProperty("vedtakene") Set<FpVedtak> vedtakene,
                     @JsonProperty("annenPartAktørId") AktørId annenPartAktørId,
                     @JsonProperty("familieHendelse") FamilieHendelse familieHendelse,
                     @JsonProperty("aksjonspunkt") Set<Aksjonspunkt> aksjonspunkt,
                     @JsonProperty("søknader") Set<FpSøknad> søknader,
                     @JsonProperty("brukerRolle") BrukerRolle brukerRolle,
                     @JsonProperty("fødteBarn") Set<AktørId> fødteBarn,
                     @JsonProperty("rettigheter") Rettigheter rettigheter) implements Sak {

    @Override
    public no.nav.foreldrepenger.common.innsyn.FpSak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag) {
        var gjeldendeVedtak = safeStream(vedtakene()).max(Comparator.comparing(FpVedtak::vedtakstidspunkt));
        var dekningsgrad = gjeldendeVedtak.map(vedtak -> vedtak.dekningsgrad().tilDto()).orElse(null);
        var fpVedtak = gjeldendeVedtak
            .map(FpVedtak::tilDto)
            .orElse(null);

        var annenPart = annenPartAktørId == null ? null : new Person(new Fødselsnummer(fødselsnummerOppslag.forAktørId(annenPartAktørId)), null);
        var kanSøkeOmEndring = gjeldendeVedtak.stream().anyMatch(FpVedtak::innvilget);
        var fh = familieHendelse() == null ? null : familieHendelse().tilDto();
        var åpenBehandling = tilÅpenBehandling();
        var sisteSøknad = safeStream(søknader).max(Comparator.comparing(FpSøknad::mottattTidspunkt));
        var sisteSøknadMottattDato = sisteSøknad
            .map(s -> s.mottattTidspunkt().toLocalDate())
            .orElse(null);
        var barna = safeStream(fødteBarn).map(b -> new Person(new Fødselsnummer(b.value()), null)).collect(Collectors.toSet());
        var gjelderAdopsjon = familieHendelse() != null && familieHendelse().gjelderAdopsjon();
        var morUføretrygd =  rettigheter.morUføretrygd();
        var harAnnenForelderTilsvarendeRettEØS =  rettigheter.annenForelderTilsvarendeRettEØS();
        var rettighetType = utledRettighetType(rettigheter, sisteSøknad.map(FpSøknad::perioder).orElse(Set.of()), gjeldendeVedtak.map(
            FpVedtak::perioder).orElse(List.of()));
        return new FpSak(saksnummer.tilDto(), avsluttet(status), sisteSøknadMottattDato, kanSøkeOmEndring, MOR.equals(brukerRolle()), gjelderAdopsjon,
            morUføretrygd, harAnnenForelderTilsvarendeRettEØS, false, rettighetType, annenPart, fh, fpVedtak, åpenBehandling, barna, dekningsgrad);
    }

    private static RettighetType utledRettighetType(Rettigheter rettigheter,
                                                    Set<FpSøknadsperiode> søknadsperioder,
                                                    List<Uttaksperiode> uttaksperioder) {
        if (rettigheter.aleneomsorg()) {
            return RettighetType.ALENEOMSORG;
        }
        if (!uttaksperioder.isEmpty()) {
            var trekkDagerMedForeldrepengerKonto = uttaksperioder.stream().anyMatch(p -> p.resultat().trekkerFraKonto(FORELDREPENGER));
            return trekkDagerMedForeldrepengerKonto ? RettighetType.BARE_SØKER_RETT : RettighetType.BEGGE_RETT;
        }
        return søknadsperioder.stream().anyMatch(sp -> FORELDREPENGER.equals(sp.konto())) ? RettighetType.BARE_SØKER_RETT : RettighetType.BEGGE_RETT;
    }

    private FpÅpenBehandling tilÅpenBehandling() {
        var søknadUnderBehandling = søknadUnderBehandling();
        return søknadUnderBehandling.map(s -> {
            var perioder = s.perioder().stream().map(FpSøknadsperiode::tilDto).toList();
            return new FpÅpenBehandling(BehandlingTilstandUtleder.utled(aksjonspunkt()), perioder);
        }).orElse(null);
    }

    private Optional<FpSøknad> søknadUnderBehandling() {
        return safeStream(søknader())
            .max(Comparator.comparing(FpSøknad::mottattTidspunkt))
            .filter(sisteSøknad -> !sisteSøknad.status().behandlet());
    }
}
