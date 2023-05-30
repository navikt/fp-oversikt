package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.domene.BrukerRolle.MOR;
import static no.nav.foreldrepenger.oversikt.domene.Konto.FORELDREPENGER;
import static no.nav.foreldrepenger.oversikt.domene.SakStatus.avsluttet;

import java.time.LocalDateTime;
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
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;


public record SakFP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId,
                     @JsonProperty("status") SakStatus status,
                     @JsonProperty("vedtak") Set<FpVedtak> vedtak,
                     @JsonProperty("annenPartAktørId") AktørId annenPartAktørId,
                     @JsonProperty("familieHendelse") FamilieHendelse familieHendelse,
                     @JsonProperty("aksjonspunkt") Set<Aksjonspunkt> aksjonspunkt,
                     @JsonProperty("søknader") Set<FpSøknad> søknader,
                     @JsonProperty("brukerRolle") BrukerRolle brukerRolle,
                     @JsonProperty("fødteBarn") Set<AktørId> fødteBarn,
                     @JsonProperty("rettigheter") Rettigheter rettigheter,
                     @JsonProperty("ønskerJustertUttakVedFødsel") boolean ønskerJustertUttakVedFødsel,
                     @JsonProperty("oppdatertTidspunkt") LocalDateTime oppdatertTidspunkt) implements ForeldrepengerSak {

    @Override
    public boolean harSakSøknad() {
        return søknader != null && !søknader.isEmpty();
    }

    @Override
    public no.nav.foreldrepenger.common.innsyn.FpSak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag) {
        var sisteSøknad = sisteSøknad();
        var gjeldendeVedtak = gjeldendeVedtak();
        var dekningsgrad = dekningsgrad();
        var fpVedtak = gjeldendeVedtak
            .map(FpVedtak::tilDto)
            .orElse(null);

        var annenPart = annenPartAktørId == null ? null : new Person(new Fødselsnummer(fødselsnummerOppslag.forAktørId(annenPartAktørId)), null);
        var kanSøkeOmEndring = gjeldendeVedtak.stream().anyMatch(FpVedtak::innvilget);
        var fh = familieHendelse() == null ? null : familieHendelse().tilDto();
        var åpenBehandling = tilÅpenBehandling(kanSøkeOmEndring);
        var sisteSøknadMottattDato = sisteSøknad
            .map(s -> s.mottattTidspunkt().toLocalDate())
            .orElse(null);
        var barna = safeStream(fødteBarn).map(b -> {
            var fnr = new Fødselsnummer(fødselsnummerOppslag.forAktørId(b));
            return new Person(fnr, null);
        }).collect(Collectors.toSet());
        var gjelderAdopsjon = familieHendelse() != null && familieHendelse().gjelderAdopsjon();
        var morUføretrygd = rettigheter != null && rettigheter.morUføretrygd();
        var harAnnenForelderTilsvarendeRettEØS = rettigheter != null && rettigheter.annenForelderTilsvarendeRettEØS();
        var rettighetType = utledRettighetType(rettigheter, sisteSøknad.map(FpSøknad::perioder).orElse(Set.of()), gjeldendeVedtak.map(
            FpVedtak::perioder).orElse(List.of()));
        return new FpSak(saksnummer.tilDto(), avsluttet(status), sisteSøknadMottattDato, kanSøkeOmEndring, MOR.equals(brukerRolle()), gjelderAdopsjon,
            morUføretrygd, harAnnenForelderTilsvarendeRettEØS, ønskerJustertUttakVedFødsel, rettighetType, annenPart, fh, fpVedtak, åpenBehandling,
            barna, dekningsgrad == null ? null : dekningsgrad.tilDto(), oppdatertTidspunkt());
    }

    @Override
    public Dekningsgrad dekningsgrad() {
        return gjeldendeVedtak().map(FpVedtak::dekningsgrad)
            .orElseGet(() -> sisteSøknad().map(FpSøknad::dekningsgrad).orElse(null));
    }

    @Override
    public Optional<FpSøknad> sisteSøknad() {
        return safeStream(søknader).max(Comparator.comparing(FpSøknad::mottattTidspunkt));
    }

    @Override
    public boolean oppgittAleneomsorg() {
        return rettigheter != null && rettigheter.aleneomsorg();
    }

    @Override
    public Optional<FpVedtak> gjeldendeVedtak() {
        return safeStream(vedtak()).max(Comparator.comparing(FpVedtak::vedtakstidspunkt));
    }

    private static RettighetType utledRettighetType(Rettigheter rettigheter,
                                                    Set<FpSøknadsperiode> søknadsperioder,
                                                    List<Uttaksperiode> uttaksperioder) {
        if (rettigheter == null) {
            return null;
        }
        if (rettigheter.aleneomsorg()) {
            return RettighetType.ALENEOMSORG;
        }
        if (!uttaksperioder.isEmpty()) {
            var trekkDagerMedForeldrepengerKonto = uttaksperioder.stream().anyMatch(p -> p.resultat().trekkerFraKonto(FORELDREPENGER));
            return trekkDagerMedForeldrepengerKonto ? RettighetType.BARE_SØKER_RETT : RettighetType.BEGGE_RETT;
        }
        if (rettigheter.annenForelderTilsvarendeRettEØS()) {
            return RettighetType.BARE_SØKER_RETT;
        }
        return søknadsperioder.stream().anyMatch(sp -> FORELDREPENGER.equals(sp.konto())) ? RettighetType.BARE_SØKER_RETT : RettighetType.BEGGE_RETT;
    }

    private FpÅpenBehandling tilÅpenBehandling(boolean kanSøkeOmEndring) {
        var søknadUnderBehandling = søknadUnderBehandling();
        return søknadUnderBehandling.map(s -> {
            //Innsyn-frontend har nå bare støtte for å vise søknadsperioder i førstegangssøknad. Bruker nå kanSøkeOmEndring for best effort å
            // bare returnere perioder i førstegangsbehandlinger
            List<UttakPeriode> perioder = kanSøkeOmEndring ? List.of() : s.perioder()
                .stream()
                .map(FpSøknadsperiode::tilDto)
                .sorted(Comparator.comparing(UttakPeriode::fom))
                .toList();
            return new FpÅpenBehandling(BehandlingTilstandUtleder.utled(aksjonspunkt()), perioder);
        }).orElse(null);
    }

    private Optional<FpSøknad> søknadUnderBehandling() {
        return safeStream(søknader())
            .max(Comparator.comparing(FpSøknad::mottattTidspunkt))
            .filter(sisteSøknad -> !sisteSøknad.status().behandlet());
    }
}
