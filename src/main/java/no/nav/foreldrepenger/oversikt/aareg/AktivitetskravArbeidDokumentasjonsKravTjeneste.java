package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
public class AktivitetskravArbeidDokumentasjonsKravTjeneste {

    private static final Set<ArbeidType> RELEVANT_ARBEID = Set.of(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ArbeidType.MARITIMT_ARBEIDSFORHOLD);

    private static final BigDecimal KRAV_FOR_DOKUMENTASJON = BigDecimal.valueOf(75);

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    public AktivitetskravArbeidDokumentasjonsKravTjeneste() {
        //CDI
    }

    @Inject
    public AktivitetskravArbeidDokumentasjonsKravTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
    }

    public boolean krevesDokumentasjonForAktivitetskravArbeid(PerioderMedAktivitetskravArbeid request) {

        // Lager en tidslinje som inneholder periodene som skal sjekkes med hensyn på behov for dokumenasjon av aktivitetskrav = arbeid.
        var requestTidslinje = request.aktivitetskravPerioder().stream()
            .map(p -> new LocalDateSegment<>(p, BigDecimal.ZERO))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())));

        // Ingen perioder i request = ikke behov for dokumentasjon
        if (requestTidslinje.isEmpty()) {
            return false;
        }

        // Slår opp i Aa-register, velger typer arbeidsforhold som er relevante, mapper om og grupperer på arbeidsgiver
        var ident = request.morFødselsnummer();
        var morsAktivitet = arbeidsforholdTjeneste.finnAlleArbeidsforholdForIdentIPerioden(ident, requestTidslinje.getMinLocalDate(), requestTidslinje.getMaxLocalDate())
            .stream()
            .filter(a -> RELEVANT_ARBEID.contains(a.arbeidsforholdIdentifikator().arbeidType()))
            .collect(Collectors.groupingBy(this::aktivitetskravNøkkel));

        // Ikke funnet noe arbeid, dokumentasjon er nødvendig
        if (morsAktivitet.isEmpty()) {
            return true;
        }

        return finnesBehovForDokumentasjon(requestTidslinje, morsAktivitet);
    }

    private boolean finnesBehovForDokumentasjon(LocalDateTimeline<BigDecimal> requestTidslinje,
                                                Map<String, List<Arbeidsforhold>> morsAktivitet) {
        Map<String, LocalDateTimeline<BigDecimal>> tidslinjerPrOrgnummer = new LinkedHashMap<>();

        var requestSpanTidslinje = new LocalDateTimeline<>(requestTidslinje.getMinLocalDate(), requestTidslinje.getMaxLocalDate(), BigDecimal.ZERO);

        // Koden nedenfor kunne vært enda mer forenklet men vi venter på litt mer erfaring med data fra Aa-register
        // Vi skal potensielt se bort fra enkelte typer permisjon eller permisjon for tilfelle med stillingsprosent 0%
        for (var entry : morsAktivitet.entrySet()) {
            var orgnr = entry.getKey();
            var arbeidsforhold = entry.getValue();
            // Sjekker om det finnes permisjoner i noen av de søkte periodene. Denne vil ha behov for tuning etterhvert (fx stilling 0% med permisjon).
            var permisjonProsentTidslinje = permisjonTidslinje(arbeidsforhold);
            var harPermisjonForRequestPerioder = permisjonProsentTidslinje.intersection(requestTidslinje).stream()
                .anyMatch(s -> s.getValue().compareTo(BigDecimal.ZERO) > 0);
            if (harPermisjonForRequestPerioder) {
                return true;
            }
            // Lager tidslinje med stillingsprosent fra Aa-register, fyller på med 0% for perioder uten arbeid og beskjærer mot request.
            var stillingsprosentTidslinje = stillingsprosentTidslinje(arbeidsforhold);
            var grunnlagTidslinje = stillingsprosentTidslinje.crossJoin(requestSpanTidslinje, StandardCombinators::coalesceLeftHandSide)
                .intersection(requestTidslinje);
            tidslinjerPrOrgnummer.put(orgnr, grunnlagTidslinje);
        }

        // Summerer opp alle stillingsprosent for hver periode og beskjærer (nok en gang) mot request.
        var summertStillingsprosent = tidslinjerPrOrgnummer.values().stream()
            .flatMap(LocalDateTimeline::stream)
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())))
            .intersection(requestTidslinje);

        // Krever dokumentasjon dersom en av request-periodene har arbeid under 75%.
        return summertStillingsprosent.stream().anyMatch(s -> s.getValue().compareTo(KRAV_FOR_DOKUMENTASJON) < 0);
    }

    private static LocalDateTimeline<BigDecimal> permisjonTidslinje(List<Arbeidsforhold> arbeidsforholdInfo) {
        return arbeidsforholdInfo.stream()
            .flatMap(a -> a.permisjoner().stream())
            .map(s -> new LocalDateSegment<>(s.getLocalDateInterval(), summerPermisjoner(s.getValue())))
            .collect(Collectors.collectingAndThen(Collectors.toList(), liste -> new LocalDateTimeline<>(liste, bigDesimalSum())));
    }

    private static LocalDateSegmentCombinator<BigDecimal, BigDecimal, BigDecimal> bigDesimalSum() {
        return StandardCombinators::sum;
    }

    private static BigDecimal summerPermisjoner(List<Permisjon> permisjoner) {
        return permisjoner.stream().map(Permisjon::permisjonsprosent).map(Stillingsprosent::prosent).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String aktivitetskravNøkkel(Arbeidsforhold arbeidsforhold) {
        return arbeidsforhold.arbeidsforholdIdentifikator().arbeidsgiver();
    }

    private LocalDateTimeline<BigDecimal> stillingsprosentTidslinje(List<Arbeidsforhold> arbeidsforholdInfo) {
        return arbeidsforholdInfo.stream()
            .flatMap(a -> a.arbeidsavtaler().stream())
            .map(a -> new LocalDateSegment<>(a.getLocalDateInterval(), a.getValue().prosent()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())));
    }

}
