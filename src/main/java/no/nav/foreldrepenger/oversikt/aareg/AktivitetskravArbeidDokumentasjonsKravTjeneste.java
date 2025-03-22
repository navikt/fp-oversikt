package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;


@ApplicationScoped
public class AktivitetskravArbeidDokumentasjonsKravTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AktivitetskravArbeidDokumentasjonsKravTjeneste.class);

    private static final Set<ArbeidType> RELEVANT_ARBEID = Set.of(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ArbeidType.MARITIMT_ARBEIDSFORHOLD);

    private static final BigDecimal KRAV_FOR_DOKUMENTASJON = BigDecimal.valueOf(75);

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private PersonOppslagSystem personOppslagSystem;

    public AktivitetskravArbeidDokumentasjonsKravTjeneste() {
        //CDI
    }

    @Inject
    public AktivitetskravArbeidDokumentasjonsKravTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste,
                                                          PersonOppslagSystem personOppslagSystem) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.personOppslagSystem = personOppslagSystem;
    }

    public boolean krevesDokumentasjonForAktivitetskravArbeid(PerioderMedAktivitetskravArbeid request) {

        var requestTidslinje = request.aktivitetskravPerioder().stream()
            .map(p -> new LocalDateSegment<>(p, BigDecimal.ZERO))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())));

        if (requestTidslinje.isEmpty()) {
            return false;
        }

        var ident = personOppslagSystem.fødselsnummer(request.morAktørId());
        var morsAktivitet = arbeidsforholdTjeneste.finnArbeidsforholdForIdentIPerioden(ident, requestTidslinje.getMinLocalDate(), requestTidslinje.getMaxLocalDate())
            .entrySet().stream()
            .map(ArbeidsforholdMapper::mapTilArbeidsforholdMedPermisjoner)
            .filter(a -> RELEVANT_ARBEID.contains(a.arbeidsforholdIdentifikator().type()))
            .collect(Collectors.groupingBy(this::aktivitetskravNøkkel));

        if (morsAktivitet.isEmpty()) {
            return true;
        }

        return finnesBehovForDokumentasjon(requestTidslinje, morsAktivitet);
    }

    private boolean finnesBehovForDokumentasjon(LocalDateTimeline<BigDecimal> requestTidslinje,
                                                Map<String, List<GruppertArbeidsforholdPrArbeidsforholdId>> morsAktivitet) {
        Map<String, LocalDateTimeline<AktivitetskravPeriodeGrunnlag>> tidslinjerPrOrgnummer = new LinkedHashMap<>();

        var requestSpanTidslinje = new LocalDateTimeline<>(requestTidslinje.getMinLocalDate(), requestTidslinje.getMaxLocalDate(),
            new AktivitetskravPeriodeGrunnlag(BigDecimal.ZERO, new LokalPermisjon(BigDecimal.ZERO, PermType.UKJENT)));

        morsAktivitet.forEach((orgnr, value) -> {
            // Lager to tidslinjer siden det kan være potensielt flere samtidige stillinger og flere samtidige permisjoner
            var stillingsprosentTidslinje = stillingsprosentTidslinje(value);
            var permisjonProsentTidslinje = permisjonTidslinje(value);
            // Kombiner stillingsprosent og permisjon - deretter kombineres med brutto request og beskjæres med netto request
            var grunnlagTidslinje = stillingsprosentTidslinje
                .crossJoin(permisjonProsentTidslinje, bigDecimalTilAktivitetskravVurderingGrunnlagCombinator())
                .crossJoin(requestSpanTidslinje, StandardCombinators::coalesceLeftHandSide)
                .intersection(requestTidslinje);
            tidslinjerPrOrgnummer.put(orgnr, grunnlagTidslinje);
        });

        var harPermisjonFraNoenArbeidsgivere = tidslinjerPrOrgnummer.values().stream()
            .flatMap(LocalDateTimeline::stream)
            .anyMatch(s -> s.getValue().permisjon().prosent().compareTo(BigDecimal.ZERO) > 0);

        if (harPermisjonFraNoenArbeidsgivere) {
            return true;
        }

        var summertStillingsprosent = tidslinjerPrOrgnummer.values().stream()
            .flatMap(LocalDateTimeline::stream)
            .map(s -> new LocalDateSegment<>(s.getLocalDateInterval(), s.getValue().sumStillingsprosent()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())))
            .intersection(requestTidslinje);

        return summertStillingsprosent.stream().anyMatch(s -> s.getValue().compareTo(KRAV_FOR_DOKUMENTASJON) < 0);
    }

    private static LocalDateTimeline<LokalPermisjon> permisjonTidslinje(List<GruppertArbeidsforholdPrArbeidsforholdId> arbeidsforholdInfo) {
        //sørger for at hull blir 0% og at de permisjonene som overlapper  per arbeidsforhold summeres
        return arbeidsforholdInfo.stream()
            .flatMap(a -> a.permisjoner().stream())
            .map(p -> new LocalDateSegment<>(p.permisjonPeriode(), new LokalPermisjon(p.permisjonsprosent(), p.permisjonstype())))
            .collect(Collectors.collectingAndThen(Collectors.toList(), liste -> new LocalDateTimeline<>(liste, permisjonSum())));
    }

    private static LocalDateSegmentCombinator<BigDecimal, BigDecimal, BigDecimal> bigDesimalSum() {
        return StandardCombinators::sum;
    }

    private static LocalDateSegmentCombinator<LokalPermisjon, LokalPermisjon, LokalPermisjon> permisjonSum() {
        return (datoInterval, datoSegment, datoSegment2) -> {
            var prosent = datoSegment != null ? datoSegment.getValue().prosent() : BigDecimal.ZERO;
            var prosent2 = datoSegment2 != null ? datoSegment2.getValue().prosent() : BigDecimal.ZERO;
            var sumType = sumType(datoSegment, datoSegment2);
            return new LocalDateSegment<>(datoInterval, new LokalPermisjon(prosent.add(prosent2), sumType));
        };
    }

    private static PermType sumType(LocalDateSegment<LokalPermisjon> datoSegment, LocalDateSegment<LokalPermisjon> datoSegment2) {
        var type1 = datoSegment != null ? datoSegment.getValue().type() : PermType.UKJENT;
        var type2 = datoSegment2 != null ? datoSegment2.getValue().type() : PermType.UKJENT;
        if (type1 != PermType.UKJENT && type2 != PermType.UKJENT && type1 != type2) {
            return PermType.PERMISJON;
        }
        return type1 != PermType.UKJENT ? type1 : type2;
    }

    private static LocalDateSegmentCombinator<BigDecimal, LokalPermisjon, AktivitetskravPeriodeGrunnlag> bigDecimalTilAktivitetskravVurderingGrunnlagCombinator() {
        return (localDateInterval, stillingsprosent, permisjon) -> {
            var sumStillingsprosent = stillingsprosent != null ? stillingsprosent.getValue() : BigDecimal.ZERO;
            var sumPermisjon = permisjon != null ? permisjon.getValue() : new LokalPermisjon(BigDecimal.ZERO, PermType.UKJENT);
            return new LocalDateSegment<>(localDateInterval, new AktivitetskravPeriodeGrunnlag(sumStillingsprosent, sumPermisjon));
        };
    }

    private String aktivitetskravNøkkel(GruppertArbeidsforholdPrArbeidsforholdId arbeidsforholdInfo) {
        return arbeidsforholdInfo.arbeidsforholdIdentifikator().arbeidsgiver();
    }

    private LocalDateTimeline<BigDecimal> stillingsprosentTidslinje(List<GruppertArbeidsforholdPrArbeidsforholdId> arbeidsforholdInfo) {
        return arbeidsforholdInfo.stream()
            .flatMap(a -> a.arbeidsavtaler().stream())
            .map(aktivitetAvtale -> new LocalDateSegment<>(aktivitetAvtale.arbeidsavtalePeriode(), aktivitetAvtale.stillingsprosent()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, bigDesimalSum())));
    }

    private record AktivitetskravPeriodeGrunnlag(BigDecimal sumStillingsprosent, LokalPermisjon permisjon) { }

    private record LokalPermisjon(BigDecimal prosent, PermType type) { }
}
