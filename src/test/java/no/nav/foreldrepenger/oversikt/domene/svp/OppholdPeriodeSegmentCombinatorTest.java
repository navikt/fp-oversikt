package no.nav.foreldrepenger.oversikt.domene.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;

class OppholdPeriodeSegmentCombinatorTest {

    @Test
    void skal_prioritere_im() {
        var combinator = new OppholdPeriodeSegmentCombinator();
        var interval = new LocalDateInterval(LocalDate.now(), LocalDate.now());
        var s1 = new LocalDateSegment<>(interval,
            new OppholdPeriode(interval.getFomDato(), interval.getTomDato(), OppholdPeriode.Årsak.FERIE, OppholdPeriode.OppholdKilde.SAKSBEHANDLER));
        var s2 = new LocalDateSegment<>(interval, new OppholdPeriode(interval.getFomDato(), interval.getTomDato(), OppholdPeriode.Årsak.FERIE,
            OppholdPeriode.OppholdKilde.INNTEKTSMELDING));
        var result = combinator.combine(interval, s1, s2);
        assertThat(result).isEqualTo(s2);
    }
}
