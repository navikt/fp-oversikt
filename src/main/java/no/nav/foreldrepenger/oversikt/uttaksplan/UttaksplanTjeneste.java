package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.Saker;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;

/**
 * Bygger en søkerrelativ felles uttaksplan for søker og annen part.
 *
 * Tjenesten eier relasjonskravet mellom søker og annen part. Kravet ligger her, ikke i
 * REST-ressursen, slik at det ikke kan omgås av senere kallsteder.
 */
@ApplicationScoped
public class UttaksplanTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(UttaksplanTjeneste.class);

    private Saker saker;
    private AnnenPartSakTjeneste annenPartSakTjeneste;
    private AnnenPartUttaksplanRepository annenPartUttaksplanRepository;

    @Inject
    public UttaksplanTjeneste(Saker saker,
                             AnnenPartSakTjeneste annenPartSakTjeneste,
                             AnnenPartUttaksplanRepository annenPartUttaksplanRepository) {
        this.saker = saker;
        this.annenPartSakTjeneste = annenPartSakTjeneste;
        this.annenPartUttaksplanRepository = annenPartUttaksplanRepository;
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
        var annenPartsSak = annenPart == null
            ? Optional.<ForeldrepengerSak>empty()
            : annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(søker, annenPart, barn, familiehendelse);

        if (søkersSak.isEmpty() && annenPartsSak.isEmpty()) {
            LOG.info("Fant verken sak for søker eller annen part. Returnerer tom uttaksplan");
            return Optional.empty();
        }

        // Metadata utledes fra søkerens sak når den finnes, ellers fra annen parts sak.
        var metadataSak = søkersSak.or(() -> annenPartsSak).orElseThrow();
        var familieHendelse = metadataSak.familieHendelse();
        var dekningsgrad = metadataSak.dekningsgrad();

        var søkerUttak = søkersSak.map(UttaksplanTjeneste::søkerensUttak).orElseGet(List::of);
        var annenPartsUttak = annenPartsUttak(annenPart, søkersSak, annenPartsSak);
        var eøsPerioder = søkersSak.map(UttaksplanTjeneste::eøsPerioder).orElseGet(List::of);

        var tidslinje = UttaksplanTidslinje.normaliser(søkerUttak, annenPartsUttak, eøsPerioder);
        var perioder = tidslinje.stream().map(LocalDateSegment::getValue).toList();
        return Optional.of(new FellesUttaksplanDto(familieHendelse == null ? null : familieHendelse.termindato(),
            familieHendelse == null ? null : familieHendelse.antallBarn(), UttaksplanMapper.mapDekningsgrad(dekningsgrad), perioder));
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

    private static List<Planperiode> søkerensUttak(ForeldrepengerSak sak) {
        var vedtaksperioder = sak.gjeldendeVedtak().map(vedtak -> UttaksplanMapper.mapVedtaksperioder(vedtak.perioder(), sak.brukerRolle()));
        if (vedtaksperioder.isPresent()) {
            return vedtaksperioder.get();
        }
        LOG.info("Søkers sak har ikke gjeldende vedtak, bruker perioder fra siste ubehandlede søknad. Saksnummer {}", sak.saksnummer().value());
        return sak.sisteSøknad()
            .filter(søknad -> !søknad.status().behandlet())
            .map(søknad -> UttaksplanMapper.mapSøknadsperioder(søknad.perioder(), sak.brukerRolle()))
            .orElseGet(List::of);
    }

    private static List<Planperiode> annenPartsUttak(ForeldrepengerSak sak) {
        return sak.gjeldendeVedtak()
            .map(vedtak -> UttaksplanMapper.mapVedtaksperioder(vedtak.perioder(), sak.brukerRolle()))
            .map(AnnenPartGraderingFilter::fjernArbeidsgivere)
            .orElseGet(List::of);
    }

    private List<Planperiode> annenPartsUttak(AktørId annenPart,
                                             Optional<ForeldrepengerSak> søkersSak,
                                             Optional<ForeldrepengerSak> annenPartsSak) {
        if (annenPart == null) {
            return List.of();
        }
        var lagretPlan = søkersSak
            .filter(sak -> Objects.equals(sak.annenPartAktørId(), annenPart))
            .flatMap(sak -> annenPartUttaksplanRepository.hentFor(sak.saksnummer()));
        if (lagretPlan.isPresent() && annenPartsSak.map(sak -> lagretPlan.get().mottattTidspunkt().isAfter(sak.oppdatertTidspunkt())).orElse(true)) {
            return lagretPlan.get().perioder();
        }
        return annenPartsSak.map(UttaksplanTjeneste::annenPartsUttak).orElseGet(List::of);
    }

    private static List<UttakPeriodeAnnenpartEøs> eøsPerioder(ForeldrepengerSak sak) {
        return sak.gjeldendeVedtak()
            .map(vedtak -> Stream.ofNullable(vedtak.annenpartEøsUttaksperioder()).flatMap(Collection::stream).toList())
            .orElseGet(List::of);
    }

}
