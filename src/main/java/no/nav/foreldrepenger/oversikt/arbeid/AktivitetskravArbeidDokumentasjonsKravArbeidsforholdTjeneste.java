package no.nav.foreldrepenger.oversikt.arbeid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.ArbeidType;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;


/**
 * Tjeneste for å vurdere om det kreves dokumentasjon for søknadsperioder med aktivitetskrav arbeid.
 * <p>
 * Krever dokumentasjon dersom det ikke finnes arbeidsforhold med stillingsprosent >= 75% for alle søknadsperiodene
 * Krever også dokumentasjon dersom det finnes permisjoner i noen av søknadsperiodene.
 */
@ApplicationScoped
public class AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste {

    private static final Set<ArbeidType> RELEVANT_ARBEID = Set.of(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ArbeidType.MARITIMT_ARBEIDSFORHOLD);

    private static final BigDecimal KRAV_FOR_DOKUMENTASJON_UTTAK = BigDecimal.valueOf(75);
    private static final BigDecimal KRAV_FOR_DOKUMENTASJON_UTSETTELSE = BigDecimal.ONE;

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    public AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste() {
        //CDI
    }

    @Inject
    public AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
    }

    public boolean krevesDokumentasjonForAktivitetskravArbeid(PerioderMedAktivitetskravArbeid request) {

        // Lager en tidslinje som inneholder periodene som skal sjekkes med hensyn på behov for dokumenasjon av aktivitetskrav = arbeid.
        var requestTidslinje = new LocalDateTimeline<>(request.aktivitetskravPerioder());

        // Ingen perioder i request = ikke behov for dokumentasjon
        if (requestTidslinje.isEmpty()) {
            return false;
        }

        // Slår opp i Aa-register, velger relevante arbeidsforhold, mapper om, grupperer på arbeidsforhold med samme arb.giver og rapportert id.
        var ident = request.morFødselsnummer();
        var morsAktivitet = arbeidsforholdTjeneste.finnAlleArbeidsforholdForIdentIPerioden(ident, requestTidslinje.getMinLocalDate(), requestTidslinje.getMaxLocalDate())
            .stream()
            .filter(a -> RELEVANT_ARBEID.contains(a.arbeidsforholdIdentifikator().arbeidType()))
            .collect(Collectors.groupingBy(Arbeidsforhold::arbeidsforholdIdentifikator));

        // Ikke funnet noe arbeid, dokumentasjon er nødvendig
        if (morsAktivitet.isEmpty()) {
            return true;
        }

        var tidslinjeUttak = request.aktivitetskravPerioder().stream()
            .filter(p -> PeriodeMedAktivitetskravType.UTTAK.equals(p.getValue()))
            .map(p -> new LocalDateSegment<>(p.getLocalDateInterval(), BigDecimal.ZERO))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())));

        var tidslinjeUtsettelse = request.aktivitetskravPerioder().stream()
            .filter(p -> PeriodeMedAktivitetskravType.UTSETTELSE.equals(p.getValue()))
            .map(p -> new LocalDateSegment<>(p.getLocalDateInterval(), BigDecimal.ZERO))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())));

        return (!tidslinjeUttak.isEmpty() && finnesBehovForDokumentasjon(tidslinjeUttak, KRAV_FOR_DOKUMENTASJON_UTTAK, morsAktivitet))
            || !tidslinjeUtsettelse.isEmpty() && finnesBehovForDokumentasjon(tidslinjeUtsettelse, KRAV_FOR_DOKUMENTASJON_UTSETTELSE, morsAktivitet);
    }

    private boolean finnesBehovForDokumentasjon(LocalDateTimeline<BigDecimal> requestTidslinje, BigDecimal minsteStillingsprosent,
                                                Map<ArbeidsforholdIdentifikator, List<Arbeidsforhold>> morsAktivitet) {
        List<LocalDateTimeline<BigDecimal>> tidslinjerPrArbeidsforhold = new ArrayList<>();

        var requestSpanTidslinje = new LocalDateTimeline<>(requestTidslinje.getMinLocalDate(), requestTidslinje.getMaxLocalDate(), BigDecimal.ZERO);

        // MorsAktivitet er gruppert pr arbeidsgiver og rapportert id. Vi lager derfor tidslinjer for slike grupperte arbeidsforhold.
        // Med litt raffinering av logikk kan det hende vi skal se på å aggregere opp til arbeidsgiver også. Kommer an på regelutformingen.
        for (var entry : morsAktivitet.entrySet()) {
            var arbeidsforholdMedSammeId = entry.getValue();
            // Sjekker om det finnes permisjoner i noen av de søkte periodene. Denne vil ha behov for tuning etterhvert (fx stilling 0% med permisjon).
            var permisjonProsentTidslinje = permisjonTidslinje(arbeidsforholdMedSammeId, requestTidslinje);
            var harPermisjonForRequestPerioder = permisjonProsentTidslinje.intersection(requestTidslinje).stream()
                .anyMatch(s -> s.getValue().compareTo(BigDecimal.ZERO) > 0);
            if (harPermisjonForRequestPerioder) {
                return true;
            }
            // Lager tidslinje med stillingsprosent fra Aa-register, fyller på med 0% for perioder uten arbeid og beskjærer mot request.
            var stillingsprosentTidslinje = stillingsprosentTidslinje(arbeidsforholdMedSammeId);
            // her skal man eventelt sammenstille med permisjon for å kunne eliminere enkelte tilfelle - se fpsak for relevant crossJoin(perm)
            var grunnlagTidslinje = stillingsprosentTidslinje.crossJoin(requestSpanTidslinje, StandardCombinators::coalesceLeftHandSide)
                .intersection(requestTidslinje);
            tidslinjerPrArbeidsforhold.add(grunnlagTidslinje);
        }

        var summertStillingsprosent = summerStillingsprosentTidslinje(requestTidslinje, tidslinjerPrArbeidsforhold);

        // Krever dokumentasjon dersom en av request-periodene har arbeid under 75%.
        return summertStillingsprosent.stream().anyMatch(s -> s.getValue().compareTo(minsteStillingsprosent) < 0);
    }

    private static LocalDateTimeline<BigDecimal> summerStillingsprosentTidslinje(LocalDateTimeline<BigDecimal> requestTidslinje,
                                                                      List<LocalDateTimeline<BigDecimal>> tidslinjerPrArbeidsforhold) {
        // Summerer opp alle stillingsprosent for hver periode og beskjærer (nok en gang) mot request.
        return tidslinjerPrArbeidsforhold.stream()
            .flatMap(LocalDateTimeline::stream)
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())))
            .intersection(requestTidslinje);
    }

    private static LocalDateTimeline<BigDecimal> permisjonTidslinje(List<Arbeidsforhold> arbeidsforholdene, LocalDateTimeline<BigDecimal> requestTidslinje) {
        return arbeidsforholdene.stream()
            .flatMap(a -> permisjonSegmenter(a, requestTidslinje))
            .collect(Collectors.collectingAndThen(Collectors.toList(), liste -> new LocalDateTimeline<>(liste, bigDesimalSum())));
    }

    private static Stream<LocalDateSegment<BigDecimal>> permisjonSegmenter(Arbeidsforhold arbeidsforhold, LocalDateTimeline<BigDecimal> requestTidslinje) {
        var stillingerSomSkalVurderePermisjon = arbeidsforhold.arbeidsavtaler()
            .filterValue(s -> s.compareTo(Stillingsprosent.ZERO) > 0)
            .intersection(requestTidslinje);
        if (stillingerSomSkalVurderePermisjon.isEmpty()) {
            return Stream.of();
        }
        var permisjoner = arbeidsforhold.permisjoner().stream()
            .map(s -> new LocalDateSegment<>(s.getLocalDateInterval(), summerPermisjoner(s.getValue())))
            .collect(Collectors.collectingAndThen(Collectors.toList(), liste -> new LocalDateTimeline<>(liste, bigDesimalSum())))
            .intersection(stillingerSomSkalVurderePermisjon);
        return permisjoner.stream();
    }

    private static LocalDateSegmentCombinator<BigDecimal, BigDecimal, BigDecimal> bigDesimalSum() {
        return StandardCombinators::sum;
    }

    private static BigDecimal summerPermisjoner(List<Permisjon> permisjoner) {
        return permisjoner.stream().map(Permisjon::permisjonsprosent).map(Stillingsprosent::prosent).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTimeline<BigDecimal> stillingsprosentTidslinje(List<Arbeidsforhold> arbeidsforholdene) {
        return arbeidsforholdene.stream()
            .flatMap(arbeidsforhold -> arbeidsforhold.arbeidsavtaler().stream())
            .map(a -> new LocalDateSegment<>(a.getLocalDateInterval(), a.getValue().prosent()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())));
    }

}
