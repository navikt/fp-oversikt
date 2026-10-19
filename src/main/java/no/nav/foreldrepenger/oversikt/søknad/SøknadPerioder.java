package no.nav.foreldrepenger.oversikt.søknad;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

final class SøknadPerioder {

    private SøknadPerioder() {
    }

    /**
     * Lager en komplett plan fra en endringssøknad. Endringsdato er første fom i endringen.
     * Eksisterende perioder for begge roller beholdes før endringsdato og kuttes der.
     * Fra endringsdato gjelder bare periodene i endringen.
     */
    static List<SøknadPeriode> medEksisterendeFørEndringsdato(List<SøknadPeriode> eksisterende, List<SøknadPeriode> endring) {
        var endringsdato = endring.stream()
            .map(SøknadPeriode::fom)
            .min(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalArgumentException("endring må ha minst én periode"));
        var førEndring = new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, endringsdato.minusDays(1));

        return Arrays.stream(Søknad.Rolle.values())
            .flatMap(rolle -> tilTimeline(eksisterende, rolle).intersection(førEndring)
                .union(tilTimeline(endring, rolle), StandardCombinators::coalesceRightHandSide)
                .stream()
                .map(SøknadPerioder::medIntervall))
            .sorted(Comparator.comparing(SøknadPeriode::fom).thenComparing(SøknadPeriode::rolle))
            .toList();
    }

    static boolean overlapperForSammeRolle(List<SøknadPeriode> perioder) {
        return Arrays.stream(Søknad.Rolle.values()).anyMatch(rolle -> {
            var sortert = perioder.stream()
                .filter(p -> p.rolle() == rolle)
                .sorted(Comparator.comparing(SøknadPeriode::fom))
                .toList();
            for (var i = 1; i < sortert.size(); i++) {
                if (!sortert.get(i).fom().isAfter(sortert.get(i - 1).tom())) {
                    return true;
                }
            }
            return false;
        });
    }

    private static LocalDateTimeline<SøknadPeriode> tilTimeline(List<SøknadPeriode> perioder, Søknad.Rolle rolle) {
        return new LocalDateTimeline<>(perioder.stream()
            .filter(p -> p.rolle() == rolle)
            .map(p -> new LocalDateSegment<>(p.fom(), p.tom(), p))
            .toList());
    }

    // Segmentets intervall er fasit. En periode som er kuttet, har fortsatt gammel fom/tom i verdien.
    private static SøknadPeriode medIntervall(LocalDateSegment<SøknadPeriode> segment) {
        var p = segment.getValue();
        LocalDate fom = segment.getFom();
        LocalDate tom = segment.getTom();
        return new SøknadPeriode(fom, tom, p.rolle(), p.kontoType(), p.utsettelseÅrsak(), p.overføringÅrsak(),
            p.morsAktivitet(), p.graderingArbeidstidprosent(), p.samtidigUttaksprosent(), p.flerbarnsdager(),
            p.aktivitetType(), p.arbeidsgiverId(), p.arbeidsgiverType());
    }
}
