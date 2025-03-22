package no.nav.foreldrepenger.oversikt.aareg;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

/**
 * Denne klassen vil mappe om en liste av kilde-arbeidsforhold til en representasjon som tar med ansettelseshistorikk.
 * Dessuten beskjæres avtaler/stillingsprosenter og permisjoner til ansettelsesperioden for det enkelte kilde-arbeidsforholdet.
 * For et gitt tidspunkt som er innenfor en av ansettelsesperiodene, så vil det være 1 stillingsprosent, men det kan være flere permisjoner.
 */
public class ArbeidsforholdMapper {

    private ArbeidsforholdMapper() {
    }

    static GruppertArbeidsforholdPrArbeidsforholdId mapTilArbeidsforholdMedPermisjoner(Map.Entry<ArbeidsforholdIdentifikator, List<Arbeidsforhold>> arbeidsforholdEntry) {
        return new GruppertArbeidsforholdPrArbeidsforholdId(
            new ArbeidsforholdIdentifikator(arbeidsforholdEntry.getKey().arbeidsgiver(), arbeidsforholdEntry.getKey().arbeidsforholdId(), arbeidsforholdEntry.getKey().type()),
            arbeidsforholdEntry.getValue().stream().map(Arbeidsforhold::ansettelsesPeriode).toList(),
            arbeidsforholdEntry.getValue().stream().map(ArbeidsforholdMapper::mapArbeidsavtaler).flatMap(Collection::stream).toList(),
            arbeidsforholdEntry.getValue().stream().map(ArbeidsforholdMapper::mapPermisjoner).flatMap(Collection::stream).toList());
    }

    private static List<Arbeidsavtale> mapArbeidsavtaler(Arbeidsforhold arbeidsforhold) {
        var ansettelse = arbeidsforhold.ansettelsesPeriode();
        var arbeidsavtalerTidlinje = arbeidsforhold.arbeidsavtaler().stream()
            .filter(arbeidsavtale -> arbeidsavtale.stillingsprosent() != null)
            .map(a -> new LocalDateSegment<>(a.arbeidsavtalePeriode(), a.stillingsprosent()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), LocalDateTimeline::new));
        return arbeidsavtalerTidlinje.intersection(ansettelse).stream()
            .map(s -> new Arbeidsavtale(s.getLocalDateInterval(), s.getValue()))
            .toList();
    }

    private static List<Permisjon> mapPermisjoner(Arbeidsforhold arbeidsforhold) {
        var ansettelse = arbeidsforhold.ansettelsesPeriode();
        var permisjonTidslinje = arbeidsforhold.permisjoner().stream()
            .filter(permisjon -> permisjon.permisjonsprosent() != null)
            .map(p -> new LocalDateSegment<>(p.permisjonPeriode(),
                List.of(new PermisjonTidslinjeObjekt(p.permisjonsprosent(), p.permisjonstype()))))
            .collect(Collectors.collectingAndThen(Collectors.toList(),
                datoSegmenter -> new LocalDateTimeline<>(datoSegmenter, StandardCombinators::concatLists)));

        return permisjonTidslinje.intersection(ansettelse).stream()
            .map(ArbeidsforholdMapper::tilPermisjonDto)
            .flatMap(Collection::stream)
            .toList();
    }

    private record PermisjonTidslinjeObjekt(Stillingsprosent permisjonsprosent, PermType permisjonstype) {}

    private static List<Permisjon> tilPermisjonDto(LocalDateSegment<List<PermisjonTidslinjeObjekt>> s) {
        return s.getValue().stream()
            .map(permisjon -> new Permisjon(s.getLocalDateInterval(), permisjon.permisjonsprosent(), permisjon.permisjonstype()))
            .toList();
    }

 }

