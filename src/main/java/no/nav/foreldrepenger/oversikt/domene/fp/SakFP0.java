package no.nav.foreldrepenger.oversikt.domene.fp;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle.MOR;
import static no.nav.foreldrepenger.oversikt.domene.fp.Konto.FORELDREPENGER;
import static no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.compress;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.FpÅpenBehandling;
import no.nav.foreldrepenger.common.innsyn.Person;
import no.nav.foreldrepenger.common.innsyn.RettighetType;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.BehandlingTilstandUtleder;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;


public record SakFP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId,
                     @JsonProperty("avsluttet") boolean avsluttet,
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
    public boolean harSøknad() {
        return søknader != null && !søknader.isEmpty();
    }

    @Override
    public boolean erKomplettForVisning() {
        return søknadUnderBehandling().map(s -> s.perioder().isEmpty()).orElse(false) || familieHendelse == null;
    }

    @Override
    public FpSak tilSakDto(PersonOppslagSystem personOppslagSystem) {
        var sisteSøknad = sisteSøknad();
        var gjeldendeVedtak = gjeldendeVedtak();
        var dekningsgrad = dekningsgrad();
        var forelder = brukerRolle.tilDto();
        var fpVedtak = gjeldendeVedtak.map(fpVedtak1 -> fpVedtak1.tilDto(forelder)).orElse(null);

        var annenPart = Optional.ofNullable(annenPartAktørId).flatMap(a -> mapPerson(personOppslagSystem, a));
        var kanSøkeOmEndring = gjeldendeVedtak.stream().anyMatch(FpVedtak::innvilget);
        var fh = familieHendelse() == null ? null : familieHendelse().tilDto();

        var åpenBehandling = tilÅpenBehandling(kanSøkeOmEndring);
        var barna = safeStream(fødteBarn).flatMap(b -> mapPerson(personOppslagSystem, b).stream()).collect(Collectors.toSet());
        var gjelderAdopsjon = familieHendelse() != null && familieHendelse().gjelderAdopsjon();
        var morUføretrygd = rettigheter != null && rettigheter.morUføretrygd();
        var harAnnenForelderTilsvarendeRettEØS = rettigheter != null && rettigheter.annenForelderTilsvarendeRettEØS();
        var rettighetType = utledRettighetType(rettigheter, sisteSøknad.map(FpSøknad::perioder).orElse(Set.of()),
            gjeldendeVedtak.map(FpVedtak::perioder).orElse(List.of()));
        return new FpSak(saksnummer.tilDto(), avsluttet, kanSøkeOmEndring, MOR.equals(brukerRolle()), gjelderAdopsjon, morUføretrygd,
            harAnnenForelderTilsvarendeRettEØS, ønskerJustertUttakVedFødsel, rettighetType, annenPart.orElse(null), fh, fpVedtak, åpenBehandling,
            barna, dekningsgrad == null ? null : dekningsgrad.tilDto(), oppdatertTidspunkt(), forelder);
    }

    private Optional<Person> mapPerson(PersonOppslagSystem personOppslagSystem, AktørId aktørId) {
        return personOppslagSystem.fødselsnummerSjekkBeskyttelse(aktørId).map(fnr -> new Person(fnr, null));
    }

    @Override
    public Dekningsgrad dekningsgrad() {
        return gjeldendeVedtak().map(FpVedtak::dekningsgrad).orElseGet(() -> sisteSøknad().map(FpSøknad::dekningsgrad).orElse(null));
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
        if (!uttaksperioder.isEmpty()) {
            var trekkDagerMedForeldrepengerKonto = uttaksperioder.stream().anyMatch(p -> p.resultat().trekkerFraKonto(FORELDREPENGER));
            if (trekkDagerMedForeldrepengerKonto) {
                return rettigheter.aleneomsorg() ? RettighetType.ALENEOMSORG : RettighetType.BARE_SØKER_RETT;
            }
            return RettighetType.BEGGE_RETT;
        }
        if (rettigheter.aleneomsorg()) {
            return RettighetType.ALENEOMSORG;
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
                .map(fpSøknadsperiode -> {
                    var forelder = brukerRolle.tilDto();
                    return fpSøknadsperiode.tilDto(forelder);
                })
                .sorted(Comparator.comparing(UttakPeriode::fom))
                .toList();
            return new FpÅpenBehandling(BehandlingTilstandUtleder.utled(aksjonspunkt()), compress(perioder));
        }).orElse(null);
    }

    private Optional<FpSøknad> søknadUnderBehandling() {
        return safeStream(søknader()).max(Comparator.comparing(FpSøknad::mottattTidspunkt)).filter(sisteSøknad -> !sisteSøknad.status().behandlet());
    }
}
