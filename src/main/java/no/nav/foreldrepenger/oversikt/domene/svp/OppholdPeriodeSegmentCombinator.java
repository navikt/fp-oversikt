package no.nav.foreldrepenger.oversikt.domene.svp;


import static no.nav.foreldrepenger.oversikt.domene.svp.OppholdPeriode.OppholdKilde.INNTEKTSMELDING;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;

public class OppholdPeriodeSegmentCombinator implements LocalDateSegmentCombinator<OppholdPeriode, OppholdPeriode, OppholdPeriode> {

    @Override
    public LocalDateSegment<OppholdPeriode> combine(LocalDateInterval interval,
                                                    LocalDateSegment<OppholdPeriode> s1,
                                                    LocalDateSegment<OppholdPeriode> s2) {
        var s1erKildeIm = s1.getValue().kilde() == INNTEKTSMELDING;
        var årsak = s1erKildeIm ? s1.getValue().årsak() : s2.getValue().årsak();
        var kilde = s1erKildeIm ? s1.getValue().kilde() : s2.getValue().kilde();
        return new LocalDateSegment<>(interval, new OppholdPeriode(interval.getFomDato(), interval.getTomDato(), årsak, kilde));
    }
}
