package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.saker.Saker;
import no.nav.foreldrepenger.oversikt.søknadsuttaksplan.Søknadsuttaksplan;
import no.nav.foreldrepenger.oversikt.søknadsuttaksplan.SøknadsuttaksplanRepository;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;

/**
 * Bygger en søkerrelativ felles uttaksplan for søker og annen part.
 *
 * Tjenesten eier relasjonskravet og adressekontrollen for annen part. Kontrollene ligger her, ikke i
 * REST-ressursen, slik at det ikke kan omgås av senere kallsteder.
 */
@ApplicationScoped
public class UttaksplanTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(UttaksplanTjeneste.class);

    private Saker saker;
    private AnnenPartSakTjeneste annenPartSakTjeneste;
    private SøknadsuttaksplanRepository søknadRepository;
    private PersonOppslagSystem personOppslagSystem;

    @Inject
    public UttaksplanTjeneste(Saker saker, AnnenPartSakTjeneste annenPartSakTjeneste, SøknadsuttaksplanRepository søknadRepository,
                             PersonOppslagSystem personOppslagSystem) {
        this.saker = saker;
        this.annenPartSakTjeneste = annenPartSakTjeneste;
        this.søknadRepository = søknadRepository;
        this.personOppslagSystem = personOppslagSystem;
    }

    UttaksplanTjeneste() {
        //CDI
    }

    /**
     * Annen part kan mangle, for eksempel ved førstegangssøknad uten oppgitt annen forelder.
     * Da bygges planen kun fra søkerens sak.
     */
    public Optional<FellesUttaksplanDto> hentFor(AktørId søker, AktørId annenPart, AktørId barn, LocalDate familiehendelse) {
        var søkersSak = søkersGjeldendeSak(søker, barn, familiehendelse);
        return hentFor(søker, annenPart, barn, familiehendelse, søkersSak);
    }

    /**
     * Brukes når søkersaken allerede er funnet via saksnummer. Barn og familiehendelse brukes
     * bare til å finne annen parts sak, aldri til å velge en annen sak for søkeren.
     */
    public Optional<FellesUttaksplanDto> hentFor(ForeldrepengerSak søkersSak) {
        var familiehendelse = Optional.ofNullable(søkersSak.familieHendelse());
        var dato = familiehendelse.map(FamilieHendelse::omsorgsovertakelse)
            .or(() -> familiehendelse.map(FamilieHendelse::fødselsdato))
            .or(() -> familiehendelse.map(FamilieHendelse::termindato))
            .orElse(null);
        var barn = søkersSak.fødteBarn() == null ? null : søkersSak.fødteBarn().stream().findFirst().orElse(null);
        return hentFor(søkersSak.aktørId(), søkersSak.annenPartAktørId(), barn, dato, Optional.of(søkersSak));
    }

    private Optional<FellesUttaksplanDto> hentFor(AktørId søker, AktørId annenPart, AktørId barn, LocalDate familiehendelse,
                                                 Optional<ForeldrepengerSak> søkersSak) {
        var visAnnenPart = annenPart != null
            && (søkersSak.isEmpty() || annenPart.equals(søkersSak.orElseThrow().annenPartAktørId()))
            && harUbeskyttetAdresse(annenPart);
        var annenPartsSak = visAnnenPart
            ? annenPartsGjeldendeSak(søker, annenPart, barn, familiehendelse, søkersSak.isEmpty())
            : Optional.<ForeldrepengerSak>empty();

        if (søkersSak.isEmpty() && annenPartsSak.isEmpty()) {
            LOG.info("Fant verken sak for søker eller annen part. Returnerer tom uttaksplan");
            return Optional.empty();
        }

        var søkerRolle = søkerRolle(søkersSak, annenPartsSak);
        var annenPartsRolle = motsattRolle(søkerRolle);

        // Søkerens søknad når søkeren har sak, ellers annen parts søknad.
        var søknad = aktivSøknad(søkersSak, annenPartsSak);
        var forventetSøknadsrolle = søkersSak.isPresent() ? søkerRolle : annenPartsRolle;
        List<Planperiode> søknadsperioder;
        if (søknad.isEmpty()) {
            søknadsperioder = legacySøknadsperioder(søkersSak);
        } else if (harRolle(søknad.orElseThrow(), forventetSøknadsrolle)) { //I tilfelle sak har endret rolle etter søknaden ble sendt
            søknadsperioder = mapSøknad(søknad.orElseThrow());
        } else {
            LOG.info("Ignorerer søknad i uttaksplan fordi søknadens rolle avviker fra sakens rolle. Bruker bare tilgjengelige vedtak.");
            søknadsperioder = List.of();
        }

        // Skjerm også annen parts perioder som allerede er lagret på søkerens sak.
        if (!visAnnenPart) {
            søknadsperioder = søknadsperioder.stream().filter(p -> p.uttak().forelder() == søkerRolle).toList();
        }
        var søknadsutsettelser = søknadsperioder.stream()
            .filter(UttaksplanTjeneste::erUtsettelse)
            .toList();

        var søkerPerioder = vedtakEllerSøknadsuttak(søkersSak, søknadsperioder, søkerRolle);
        var annenPartsPerioder = visAnnenPart ? AnnenPartGraderingFilter.fjernArbeidsgivere(
            vedtakEllerSøknadsuttak(annenPartsSak, søknadsperioder, annenPartsRolle)) : List.<Planperiode>of();
        var eøsPerioder = visAnnenPart ? eøsPerioder(søkersSak) : List.<UttakPeriodeAnnenpartEøs>of();

        // Metadata utledes fra søkerens sak når den finnes, ellers fra annen parts sak.
        var metadataSak = søkersSak.or(() -> annenPartsSak).orElseThrow();
        return Optional.of(lagPlan(metadataSak, søkerRolle, søkerPerioder, annenPartsPerioder, eøsPerioder,
            søknadsutsettelser));
    }

    private boolean harUbeskyttetAdresse(AktørId annenPart) {
        Fødselsnummer fnr;
        try {
            fnr = personOppslagSystem.fødselsnummer(annenPart);
        } catch (NoSuchElementException e) {
            // Identoppslaget bruker Optional.orElseThrow() når PDL ikke finner personen.
            return false;
        }
        if (fnr == null) {
            return false;
        }
        try {
            var beskyttelse = personOppslagSystem.adresseBeskyttelse(fnr);
            return beskyttelse != null && !beskyttelse.harBeskyttetAdresse();
        } catch (BrukerIkkeFunnetIPdlException e) {
            return false;
        }
    }

    private Optional<ForeldrepengerSak> annenPartsGjeldendeSak(AktørId søker, AktørId annenPart, AktørId barn,
                                                              LocalDate familiehendelse, boolean kreverEntydigSak) {
        if (kreverEntydigSak) {
            var treff = foreldrepengesaker(annenPart).stream()
                .filter(sak -> !sak.oppgittAleneomsorg())
                .filter(sak -> søker.equals(sak.annenPartAktørId()))
                .filter(sak -> gjelderSammeBarn(barn, familiehendelse, sak))
                .toList();
            return treff.size() == 1 ? Optional.of(treff.getFirst()) : Optional.empty();
        }
        return annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(søker, annenPart, barn, familiehendelse);
    }

    private static FellesUttaksplanDto.Rolle søkerRolle(Optional<ForeldrepengerSak> søkersSak,
                                                       Optional<ForeldrepengerSak> annenPartsSak) {
        var rolle = søkersSak.map(sak -> dtoRolle(sak.brukerRolle())).orElseGet(() -> annenPartsSak
            .map(sak -> motsattRolle(dtoRolle(sak.brukerRolle()))).orElseThrow());
        if (annenPartsSak.map(sak -> dtoRolle(sak.brukerRolle()) == rolle).orElse(false)) {
            throw ugyldigRolle();
        }
        return rolle;
    }

    /**
     * Annen parts søknad slås aldri opp når søkeren har egen sak.
     */
    private Optional<Søknadsuttaksplan> aktivSøknad(Optional<ForeldrepengerSak> søkersSak, Optional<ForeldrepengerSak> annenPartsSak) {
        return søkersSak.or(() -> annenPartsSak).flatMap(this::hentAktivSøknad);
    }

    /**
     * Bare søkerens egen legacy-søknad brukes. Annen parts legacy-søknad brukes aldri.
     */
    // TODO: Fjern bruk av legacy-søknad når de fleste saker har vedtak eller søknad i søknadstabellen.
    private static List<Planperiode> legacySøknadsperioder(Optional<ForeldrepengerSak> søkersSak) {
        return søkersSak.flatMap(sak -> sak.sisteSøknad()
                .map(legacy -> UttaksplanMapper.mapSøknadsperioder(legacy.perioder(), sak.brukerRolle())))
            .orElseGet(List::of)
            .stream().toList();
    }

    private List<ForeldrepengerSak> foreldrepengesaker(AktørId aktørId) {
        return saker.hentSaker(aktørId)
            .stream()
            .filter(ForeldrepengerSak.class::isInstance)
            .map(ForeldrepengerSak.class::cast)
            .toList();
    }

    /**
     * Søkerens siste sak er ikke nødvendigvis den som gjelder. En bruker kan ha saker for flere
     * barn samtidig, så saken må matches på barn eller familiehendelse, på samme måte som for
     * annen part. Ved flere treff for samme barn velges saken med sist mottatte søknad.
     *
     * I motsetning til annen part filtreres det ikke på aleneomsorg eller oppgitt medforelder:
     * søkeren har alltid tilgang til sine egne saker.
     */
    private Optional<ForeldrepengerSak> søkersGjeldendeSak(AktørId søker, AktørId barn, LocalDate familiehendelse) {
        var fpSaker = foreldrepengesaker(søker);
        if (fpSaker.isEmpty()) {
            LOG.info("Søker har ingen saker om foreldrepenger");
            return Optional.empty();
        }

        var sakerForBarnet = fpSaker.stream().filter(sak -> gjelderSammeBarn(barn, familiehendelse, sak)).toList();
        if (sakerForBarnet.isEmpty()) {
            LOG.info("Søker har ingen sak som matcher oppgitt barn eller familiehendelse");
            return Optional.empty();
        }
        if (sakerForBarnet.size() > 1) {
            var saksnummer = sakerForBarnet.stream().map(sak -> sak.saksnummer().value()).toList();
            LOG.warn("Fant flere saker for søker på samme barn. Velger sak med siste søknad. Saksnummer {}", saksnummer);
        }
        return sakerForBarnet.stream()
            .filter(sak -> sak.sisteSøknad().isPresent())
            .max(Comparator.comparing(sak -> sak.sisteSøknad().orElseThrow().mottattTidspunkt()));
    }

    public static boolean gjelderSammeBarn(AktørId barn, LocalDate familiehendelse, ForeldrepengerSak sak) {
        if (barn != null && sak.gjelderBarn(barn)) {
            return true;
        }
        if (familiehendelse == null || sak.familieHendelse() == null) {
            return false;
        }
        var fh = sak.familieHendelse();
        var gjeldende = Optional.ofNullable(fh.omsorgsovertakelse())
            .or(() -> Optional.ofNullable(fh.fødselsdato()))
            .or(() -> Optional.ofNullable(fh.termindato()))
            .orElse(null);
        if (gjeldende == null) {
            return false; // Sett i prod at dette har oppstått
        }
        return new LocalDateInterval(gjeldende.minusWeeks(5), gjeldende.plusWeeks(5)).contains(familiehendelse);
    }

    private Optional<Søknadsuttaksplan> hentAktivSøknad(ForeldrepengerSak sak) {
        return søknadRepository.hentAktivFor(sak.saksnummer());
    }

    private static boolean harRolle(Søknadsuttaksplan søknad, FellesUttaksplanDto.Rolle rolle) {
        if (søknad.rolle() == null) {
            throw ugyldigRolle();
        }
        return søknad.rolle() == søknadRolle(rolle);
    }

    private static List<Planperiode> mapSøknad(Søknadsuttaksplan søknad) {
        return UttaksplanMapper.mapLagredeSøknadsperioder(søknad.perioder());
    }

    private static List<Planperiode> mapVedtaksperioder(FpVedtak vedtak, ForeldrepengerSak sak) {
        return UttaksplanMapper.mapVedtaksperioder(vedtak.perioder(), sak.brukerRolle()).stream()
            .sorted(Comparator.comparing(Planperiode::fom).thenComparing(Planperiode::tom))
            .toList();
    }

    private static List<Planperiode> vedtakEllerSøknadsuttak(Optional<ForeldrepengerSak> sak,
                                                             List<Planperiode> søknadsperioder,
                                                             FellesUttaksplanDto.Rolle rolle) {
        var vedtak = sak.flatMap(ForeldrepengerSak::gjeldendeVedtak);
        if (vedtak.isPresent()) {
            return mapVedtaksperioder(vedtak.orElseThrow(), sak.orElseThrow());
        }
        return søknadsperioder.stream()
            .filter(p -> p.uttak().forelder() == rolle && erUttak(p))
            .sorted(Comparator.comparing(Planperiode::fom).thenComparing(Planperiode::tom))
            .toList();
    }

    private static boolean erUttak(Planperiode periode) {
        return !erUtsettelse(periode);
    }

    private static boolean erUtsettelse(Planperiode periode) {
        return periode.uttak().utsettelseÅrsak() != null;
    }

    private static FellesUttaksplanDto.Rolle dtoRolle(BrukerRolle rolle) {
        return switch (rolle) {
            case MOR -> FellesUttaksplanDto.Rolle.MOR;
            case FAR, MEDMOR -> FellesUttaksplanDto.Rolle.FAR_MEDMOR;
            case UKJENT -> throw ugyldigRolle();
        };
    }

    private static FellesUttaksplanDto.Rolle motsattRolle(FellesUttaksplanDto.Rolle rolle) {
        return rolle == FellesUttaksplanDto.Rolle.MOR ? FellesUttaksplanDto.Rolle.FAR_MEDMOR : FellesUttaksplanDto.Rolle.MOR;
    }

    private static Søknadsuttaksplan.Rolle søknadRolle(FellesUttaksplanDto.Rolle rolle) {
        return rolle == FellesUttaksplanDto.Rolle.MOR ? Søknadsuttaksplan.Rolle.MOR : Søknadsuttaksplan.Rolle.FAR_MEDMOR;
    }

    private static IllegalStateException ugyldigRolle() {
        return new IllegalStateException("Ugyldig rolle i uttaksplan");
    }

    private static List<UttakPeriodeAnnenpartEøs> eøsPerioder(FpVedtak vedtak) {
        return Stream.ofNullable(vedtak.annenpartEøsUttaksperioder()).flatMap(Collection::stream)
                .sorted(Comparator.comparing(UttakPeriodeAnnenpartEøs::fom).thenComparing(UttakPeriodeAnnenpartEøs::tom)).toList();
    }

    private static List<UttakPeriodeAnnenpartEøs> eøsPerioder(Optional<ForeldrepengerSak> søkersSak) {
        return søkersSak.flatMap(ForeldrepengerSak::gjeldendeVedtak)
            .map(UttaksplanTjeneste::eøsPerioder)
            .orElseGet(List::of);
    }

    private static FellesUttaksplanDto lagPlan(ForeldrepengerSak metadataSak,
                                               FellesUttaksplanDto.Rolle søkerRolle,
                                               List<Planperiode> søkerPerioder,
                                               List<Planperiode> annenPartsPerioder,
                                               List<UttakPeriodeAnnenpartEøs> eøsPerioder,
                                               List<Planperiode> søknadsutsettelser) {
        var familieHendelse = metadataSak.familieHendelse();
        var tidslinje = UttaksplanTidslinje.normaliser(søkerPerioder, annenPartsPerioder, eøsPerioder);
        var utfyltTidslinje = UttaksplanTidslinje.fyllHullMedUtsettelser(tidslinje, søknadsutsettelser, søkerRolle);
        var perioder = utfyltTidslinje.stream().map(LocalDateSegment::getValue).toList();
        return new FellesUttaksplanDto(familieHendelse == null ? null : familieHendelse.termindato(),
            familieHendelse == null ? null : familieHendelse.antallBarn(), UttaksplanMapper.mapDekningsgrad(metadataSak.dekningsgrad()), perioder);
    }

}
