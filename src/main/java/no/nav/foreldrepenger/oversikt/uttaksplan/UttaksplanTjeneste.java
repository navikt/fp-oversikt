package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.Saker;
import no.nav.foreldrepenger.oversikt.soknadsplan.Søknadsplan;
import no.nav.foreldrepenger.oversikt.soknadsplan.SøknadsplanRepository;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;

/**
 * Bygger en søkerrelativ felles uttaksplan for søker og annen part.
 *
 * Adressekontrollen for annen part gjøres av kalleren og uttrykkes med {@link VerifisertAnnenPart}.
 * Tjenesten eier relasjonskravet: annen part må være den samme som er registrert på søkerens sak.
 * Annen parts perioder som søkeren selv har sendt inn i sin søknadsplan, vises alltid.
 * Når begge er far og bare den ene har mødrekvote, vises han som MOR og den andre som FAR_MEDMOR. Ellers vises søkeren som FAR_MEDMOR og den andre som MOR.
 */
@ApplicationScoped
public class UttaksplanTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(UttaksplanTjeneste.class);

    private Saker saker;
    private AnnenPartSakTjeneste annenPartSakTjeneste;
    private SøknadsplanRepository søknadRepository;

    @Inject
    public UttaksplanTjeneste(Saker saker, AnnenPartSakTjeneste annenPartSakTjeneste, SøknadsplanRepository søknadRepository) {
        this.saker = saker;
        this.annenPartSakTjeneste = annenPartSakTjeneste;
        this.søknadRepository = søknadRepository;
    }

    UttaksplanTjeneste() {
        //CDI
    }

    /**
     * Annen part er null når den ikke er oppgitt, er ukjent eller har beskyttet adresse. Da hentes ikke annen parts
     * sak, og planen bygges fra søkerens sak og søkerens egen søknadsplan.
     */
    public Optional<FellesUttaksplanDto> hentFor(AktørId søker, VerifisertAnnenPart annenPart, AktørId barn, LocalDate familiehendelse) {
        var søkersSak = søkersGjeldendeSak(søker, barn, familiehendelse);
        var visAnnenPart = kanViseAnnenPart(annenPart, søkersSak);
        var annenPartsSak = visAnnenPart
            ? annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(søker, annenPart.aktørId(), barn, familiehendelse)
            : Optional.<ForeldrepengerSak>empty();

        if (søkersSak.isEmpty() && annenPartsSak.isEmpty()) {
            LOG.info("Fant verken sak for søker eller annen part. Returnerer tom uttaksplan");
            return Optional.empty();
        }

        var søkerRolle = søkerRolle(søkersSak, annenPartsSak);
        var annenPartsRolle = motsattRolle(søkerRolle);
        // Metadata utledes fra søkerens sak når den finnes, ellers fra annen parts sak.
        var metadataSak = søkersSak.or(() -> annenPartsSak).orElseThrow();
        var familieHendelse = metadataSak.familieHendelse();
        // Forslag om uttak ved fødsel gjelder ikke far og far
        var morOgFarMedmor = Stream.of(søkersSak, annenPartsSak).flatMap(Optional::stream)
            .anyMatch(sak -> sak.brukerRolle() == BrukerRolle.MOR);

        var søknadsperioder = søknadsperioder(søkersSak, annenPartsSak, søkerRolle);
        var søknadsutsettelser = søknadsperioder.stream()
            .filter(UttaksplanTjeneste::erUtsettelse)
            .toList();

        var søkerPerioder = vedtakEllerSøknadsuttak(søkersSak, søknadsperioder, søkerRolle);
        if (søkersSak.isEmpty()) {
            // Søkerens perioder kommer da fra annen parts søknad og er bare et forslag. Uttak som er klart for gammelt
            // til å bli innvilget, vises ikke. Filteret har bevisst slakk i forhold til fpsak, se SøknadsfristFilter.
            søkerPerioder = SøknadsfristFilter.fjernUttakFørFrist(søkerPerioder, LocalDate.now());
            if (morOgFarMedmor && søkerRolle == FellesUttaksplanDto.Rolle.FAR_MEDMOR) {
                // Søknadsfrontend spør far om uttak ved fødsel og legger inn ukene selv
                søkerPerioder = FødselspermisjonForslagFilter.fjernForslagFraTermin(søkerPerioder, familieHendelse);
            }
        }
        // Når annen part ikke vises, er annenPartsSak tom, og annen parts perioder kommer bare fra søkerens egen søknadsplan.
        var annenPartsUttak = vedtakEllerSøknadsuttak(annenPartsSak, søknadsperioder, annenPartsRolle);
        var annenPartHarVedtak = annenPartsSak.flatMap(ForeldrepengerSak::gjeldendeVedtak).isPresent();
        if (morOgFarMedmor && søkersSak.isPresent() && annenPartsRolle == FellesUttaksplanDto.Rolle.FAR_MEDMOR && !annenPartHarVedtak) {
            // Uten vedtak kommer fars perioder fra mors søknadsplan
            annenPartsUttak = FødselspermisjonForslagFilter.flyttForslagTilMorsSamtidigeUttak(annenPartsUttak, søkerPerioder, familieHendelse);
        }
        var annenPartsPerioder = AnnenPartGraderingFilter.fjernArbeidsgivere(annenPartsUttak);
        // EØS-perioder kommer fra søkerens eget vedtak.
        var eøsPerioder = eøsPerioder(søkersSak);

        var annenPartRedigerbar = annenPartsSak.isEmpty(); //Saker uten søknad eller henlagte er allerede filtrert bort
        return Optional.of(new FellesUttaksplanDto(familieHendelse == null ? null : familieHendelse.termindato(),
            familieHendelse == null ? null : familieHendelse.antallBarn(), UttaksplanMapper.mapDekningsgrad(metadataSak.dekningsgrad()),
            perioder(søkerRolle, søkerPerioder, annenPartsPerioder, eøsPerioder, søknadsutsettelser), annenPartRedigerbar));
    }

    /**
     * Annen parts sak hentes bare når annen part er verifisert av kalleren og er den samme som er registrert
     * på søkerens sak.
     */
    private static boolean kanViseAnnenPart(VerifisertAnnenPart annenPart, Optional<ForeldrepengerSak> søkersSak) {
        if (annenPart == null) {
            return false;
        }
        return søkersSak.map(sak -> annenPart.aktørId().equals(sak.annenPartAktørId())).orElse(true);
    }

    /**
     * Søkerens søknad når søkeren har sak, ellers annen parts søknad. Annen parts søknad brukes bare når
     * annen part vises, fordi annenPartsSak ellers er tom.
     */
    private List<Planperiode> søknadsperioder(Optional<ForeldrepengerSak> søkersSak, Optional<ForeldrepengerSak> annenPartsSak,
                                                  FellesUttaksplanDto.Rolle søkerRolle) {
        var søknad = aktivSøknad(søkersSak, annenPartsSak);
        if (søknad.isEmpty()) {
            return legacySøknadsperioder(søkersSak);
        }
        var forventetSøknadsrolle = søkersSak.or(() -> annenPartsSak)
            .map(sak -> dtoRolle(sak.brukerRolle()))
            .orElseThrow();
        if (!harRolle(søknad.orElseThrow(), forventetSøknadsrolle)) { // I tilfelle saken har endret rolle etter at søknaden ble sendt
            LOG.info("Ignorerer søknad i uttaksplan fordi søknadens rolle avviker fra sakens rolle. Bruker bare tilgjengelige vedtak.");
            return List.of();
        }
        return mapSøknad(søknad.orElseThrow());
    }

    private static FellesUttaksplanDto.Rolle søkerRolle(Optional<ForeldrepengerSak> søkersSak,
                                                       Optional<ForeldrepengerSak> annenPartsSak) {
        var rolle = søkersSak.map(sak -> dtoRolle(sak.brukerRolle())).orElseGet(() -> annenPartsSak
            .map(sak -> motsattRolle(dtoRolle(sak.brukerRolle()))).orElseThrow());
        if (annenPartsSak.map(sak -> dtoRolle(sak.brukerRolle()) == rolle).orElse(false)) {
            if (søkersSak.orElseThrow().brukerRolle() == BrukerRolle.FAR && annenPartsSak.orElseThrow().brukerRolle() == BrukerRolle.FAR) {
                return harEgenMødrekvote(søkersSak.orElseThrow()) && !harEgenMødrekvote(annenPartsSak.orElseThrow())
                    ? FellesUttaksplanDto.Rolle.MOR
                    : FellesUttaksplanDto.Rolle.FAR_MEDMOR;
            }
            throw new IllegalStateException(String.format("Ugyldig rolle i uttaksplan. Søker og annen part har samme rolle %s. Søkers saksnummer %s, annen parts saksnummer %s",
                rolle, søkersSak.orElseThrow().saksnummer().value(), annenPartsSak.orElseThrow().saksnummer().value()));
        }
        return rolle;
    }

    private static boolean harEgenMødrekvote(ForeldrepengerSak sak) {
        return sak.gjeldendeVedtak()
            .map(vedtak -> UttaksplanMapper.mapVedtaksperioder(vedtak.perioder(), sak.brukerRolle()))
            .map(UttaksplanTjeneste::harMødrekvote)
            .orElseGet(() -> sak.sisteSøknad()
                .map(søknad -> UttaksplanMapper.mapSøknadsperioder(søknad.perioder(), sak.brukerRolle()))
                .map(UttaksplanTjeneste::harMødrekvote)
                .orElse(false));
    }

    private static boolean harMødrekvote(List<Planperiode> perioder) {
        return perioder.stream()
            .anyMatch(periode -> periode.uttak().kontoType() == KontoType.MØDREKVOTE && periode.uttak().overføringÅrsak() == null);
    }

    /**
     * Annen parts søknad slås aldri opp når søkeren har egen sak.
     */
    private Optional<Søknadsplan> aktivSøknad(Optional<ForeldrepengerSak> søkersSak, Optional<ForeldrepengerSak> annenPartsSak) {
        return søkersSak.or(() -> annenPartsSak).flatMap(this::hentAktivSøknad);
    }

    /**
     * Bare søkerens egen legacy-søknad brukes. Annen parts legacy-søknad brukes aldri.
     * Uten vedtak brukes legacy-søknaden bare når den ikke er behandlet, slik som før søknadsplan ble innført.
     * Med vedtak brukes den for å fylle hull med utsettelser.
     */
    // TODO: Fjern bruk av legacy-søknad når de fleste saker har vedtak eller søknad i søknadstabellen.
    private static List<Planperiode> legacySøknadsperioder(Optional<ForeldrepengerSak> søkersSak) {
        return søkersSak.flatMap(sak -> sak.sisteSøknad()
                .filter(legacy -> sak.gjeldendeVedtak().isPresent() || !legacy.status().behandlet())
                .map(legacy -> UttaksplanMapper.mapSøknadsperioder(legacy.perioder(), sak.brukerRolle())))
            .orElseGet(List::of);
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

    private Optional<Søknadsplan> hentAktivSøknad(ForeldrepengerSak sak) {
        return søknadRepository.hentAktivFor(sak.saksnummer());
    }

    private static boolean harRolle(Søknadsplan søknad, FellesUttaksplanDto.Rolle rolle) {
        if (søknad.rolle() == null) {
            throw ugyldigRolle();
        }
        return UttaksplanMapper.mapRolle(søknad.rolle()) == rolle;
    }

    private static List<Planperiode> mapSøknad(Søknadsplan søknad) {
        return UttaksplanMapper.mapLagredeSøknadsperioder(søknad.perioder());
    }

    private static List<Planperiode> mapVedtaksperioder(FpVedtak vedtak, ForeldrepengerSak sak, FellesUttaksplanDto.Rolle rolle) {
        return UttaksplanMapper.mapVedtaksperioder(vedtak.perioder(), sak.brukerRolle()).stream()
            .map(periode -> medForelder(periode, rolle))
            .sorted(Comparator.comparing(Planperiode::fom).thenComparing(Planperiode::tom))
            .toList();
    }

    private static Planperiode medForelder(Planperiode periode, FellesUttaksplanDto.Rolle rolle) {
        var uttak = periode.uttak();
        if (uttak.forelder() == rolle) {
            return periode;
        }
        // Ved far og far har begge sakene rollen FAR, men faren med mødrekvote vises som MOR.
        // Vedtaksperiodene får derfor rollen i planen i stedet for sakens rolle. Kvoten endres ikke.
        var nyttUttak = new FellesUttaksplanDto.UttakDto(rolle, uttak.kontoType(), uttak.utsettelseÅrsak(), uttak.overføringÅrsak(),
            uttak.gradering(), uttak.morsAktivitet(), uttak.samtidigUttak(), uttak.flerbarnsdager(), uttak.resultat());
        return new Planperiode(periode.fom(), periode.tom(), nyttUttak);
    }

    private static List<Planperiode> vedtakEllerSøknadsuttak(Optional<ForeldrepengerSak> sak,
                                                             List<Planperiode> søknadsperioder,
                                                             FellesUttaksplanDto.Rolle rolle) {
        var vedtak = sak.flatMap(ForeldrepengerSak::gjeldendeVedtak);
        if (vedtak.isPresent()) {
            return mapVedtaksperioder(vedtak.orElseThrow(), sak.orElseThrow(), rolle);
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

    private static IllegalStateException ugyldigRolle() {
        return new IllegalStateException("Ugyldig rolle i uttaksplan");
    }

    private static List<UttakPeriodeAnnenpartEøs> eøsPerioder(Optional<ForeldrepengerSak> søkersSak) {
        return søkersSak.flatMap(ForeldrepengerSak::gjeldendeVedtak)
            .stream()
            .flatMap(vedtak -> Stream.ofNullable(vedtak.annenpartEøsUttaksperioder()))
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(UttakPeriodeAnnenpartEøs::fom).thenComparing(UttakPeriodeAnnenpartEøs::tom))
            .toList();
    }

    private static List<FellesUttaksplanDto.UttakPeriodeDto> perioder(FellesUttaksplanDto.Rolle søkerRolle,
                                                 List<Planperiode> søkerPerioder,
                                                 List<Planperiode> annenPartsPerioder,
                                                 List<UttakPeriodeAnnenpartEøs> eøsPerioder,
                                                 List<Planperiode> søknadsutsettelser) {
        var tidslinje = UttaksplanTidslinje.normaliser(søkerPerioder, annenPartsPerioder, eøsPerioder);
        var utfyltTidslinje = UttaksplanTidslinje.fyllHullMedUtsettelser(tidslinje, søknadsutsettelser, søkerRolle);
        return utfyltTidslinje.stream().map(LocalDateSegment::getValue).toList();
    }

}
