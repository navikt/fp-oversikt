package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class ArbeidsforholdDtoTjeneste {

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private PersonOppslagSystem personOppslagSystem;

    ArbeidsforholdDtoTjeneste() {
    }

    @Inject
    public ArbeidsforholdDtoTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste,
                                     PersonOppslagSystem personOppslagSystem) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.personOppslagSystem = personOppslagSystem;
    }

    public List<GruppertArbeidsforholdPrArbeidsforholdId> mapArbForholdOgPermisjoner(AktørId aktørId, LocalDate fom, LocalDate tom) {
        var ident = personOppslagSystem.fødselsnummer(aktørId);
        var arbeidsforhold = arbeidsforholdTjeneste.finnArbeidsforholdForIdentIPerioden(ident, fom, tom != null ? tom : LocalDate.MAX);

        return arbeidsforhold.entrySet().stream().map(this::mapTilArbeidsforholdMedPermisjoner).collect(Collectors.toList());
    }

    private GruppertArbeidsforholdPrArbeidsforholdId mapTilArbeidsforholdMedPermisjoner(Map.Entry<ArbeidsforholdIdentifikator, List<Arbeidsforhold>> arbeidsforholdEntry) {
        return new GruppertArbeidsforholdPrArbeidsforholdId(
            new ArbeidsforholdIdentifikator(arbeidsforholdEntry.getKey().arbeidsgiver(), arbeidsforholdEntry.getKey().arbeidsforholdId(), arbeidsforholdEntry.getKey().type()),
            arbeidsforholdEntry.getValue().stream().map(Arbeidsforhold::ansettelsesPeriode).toList(),
            tilArbeidsavtaler(arbeidsforholdEntry.getValue()),
            tilPermisjoner(arbeidsforholdEntry.getValue()));
    }

    private List<Permisjon> tilPermisjoner(List<Arbeidsforhold> arbeidsforhold) {
        return arbeidsforhold.stream().map(this::mapPermisjoner).flatMap(Collection::stream).toList();
    }

    private List<Arbeidsavtale> tilArbeidsavtaler(List<Arbeidsforhold> arbeidsforhold) {
        return arbeidsforhold.stream().map(this::mapArbeidsavtaler).flatMap(Collection::stream).toList();
    }

    private List<Arbeidsavtale> mapArbeidsavtaler(Arbeidsforhold arbeidsforhold) {
        var ansettelse = arbeidsforhold.ansettelsesPeriode();
        var arbeidsavtalerTidlinje = arbeidsforhold.arbeidsavtaler().stream()
            .filter(arbeidsavtale -> arbeidsavtale.stillingsprosent() != null)
            .map(a -> new LocalDateSegment<>(a.arbeidsavtalePeriode(), a.stillingsprosent()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), LocalDateTimeline::new));
        return arbeidsavtalerTidlinje.intersection(ansettelse).stream()
            .map(s -> new Arbeidsavtale(s.getLocalDateInterval(), s.getValue()))
            .toList();
    }

    private List<Permisjon> mapPermisjoner(Arbeidsforhold arbeidsforhold) {
        var ansettelse = arbeidsforhold.ansettelsesPeriode();
        var permisjonTidslinje = arbeidsforhold.permisjoner().stream()
            .filter(permisjon -> permisjon.permisjonsprosent() != null)
            .map(p -> new LocalDateSegment<>(p.permisjonPeriode(),
                List.of(new PermisjonTidslinjeObjekt(p.permisjonsprosent(), p.permisjonsÅrsak()))))
            .collect(Collectors.collectingAndThen(Collectors.toList(),
                datoSegmenter -> new LocalDateTimeline<>(datoSegmenter, StandardCombinators::concatLists)));

        return permisjonTidslinje.intersection(ansettelse)
            .stream()
            .map(ArbeidsforholdDtoTjeneste::tilPermisjonDto)
            .flatMap(Collection::stream)
            .toList();
    }

    private record PermisjonTidslinjeObjekt(BigDecimal permisjonsprosent, String permisjonsÅrsak) {}

    private static List<Permisjon> tilPermisjonDto(LocalDateSegment<List<PermisjonTidslinjeObjekt>> s) {
        return s.getValue().stream()
            .map(permisjon -> new Permisjon(s.getLocalDateInterval(), permisjon.permisjonsprosent(), permisjon.permisjonsÅrsak()))
            .toList();
    }

 }

