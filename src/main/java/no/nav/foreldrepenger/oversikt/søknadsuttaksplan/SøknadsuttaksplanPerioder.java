package no.nav.foreldrepenger.oversikt.søknadsuttaksplan;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

final class SøknadsuttaksplanPerioder {

    private SøknadsuttaksplanPerioder() {
    }

    /**
     * Lager en komplett uttaksplan fra en endringssøknad. Endringsdato er første fom i endringen.
     * Eksisterende perioder for begge roller beholdes før endringsdato og kuttes der.
     * Fra endringsdato gjelder bare periodene i endringen.
     */
    static List<SøknadsuttaksplanPeriode> medEksisterendeFørEndringsdato(List<SøknadsuttaksplanPeriode> eksisterende, List<SøknadsuttaksplanPeriode> endring) {
        var endringsdato = endring.stream()
            .map(SøknadsuttaksplanPeriode::fom)
            .min(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalArgumentException("endring må ha minst én periode"));
        var førEndring = new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, endringsdato.minusDays(1));

        return Arrays.stream(Søknadsuttaksplan.Rolle.values())
            .flatMap(rolle -> tilTimeline(eksisterende, rolle).intersection(førEndring)
                .union(tilTimeline(endring, rolle), StandardCombinators::coalesceRightHandSide)
                .stream()
                .map(SøknadsuttaksplanPerioder::medIntervall))
            .sorted(Comparator.comparing(SøknadsuttaksplanPeriode::fom).thenComparing(SøknadsuttaksplanPeriode::rolle))
            .toList();
    }

    private static LocalDateTimeline<SøknadsuttaksplanPeriode> tilTimeline(List<SøknadsuttaksplanPeriode> perioder, Søknadsuttaksplan.Rolle rolle) {
        return new LocalDateTimeline<>(perioder.stream()
            .filter(p -> p.rolle() == rolle)
            .map(p -> new LocalDateSegment<>(p.fom(), p.tom(), p))
            .toList());
    }

    // Segmentets intervall er fasit. En periode som er kuttet, har fortsatt gammel fom/tom i verdien.
    private static SøknadsuttaksplanPeriode medIntervall(LocalDateSegment<SøknadsuttaksplanPeriode> segment) {
        var p = segment.getValue();
        LocalDate fom = segment.getFom();
        LocalDate tom = segment.getTom();
        return new SøknadsuttaksplanPeriode(fom, tom, p.rolle(), p.kontoType(), p.utsettelseÅrsak(), p.overføringÅrsak(),
            p.morsAktivitet(), p.graderingArbeidstidprosent(), p.samtidigUttaksprosent(), p.flerbarnsdager(),
            p.aktivitetType(), p.arbeidsgiverId(), p.arbeidsgiverType());
    }
}
