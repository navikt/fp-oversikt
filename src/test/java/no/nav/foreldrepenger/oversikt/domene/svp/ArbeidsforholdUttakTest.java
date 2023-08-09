package no.nav.foreldrepenger.oversikt.domene.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArbeidsforholdUttakTest {

    @Test
    void fjerner_overlappende_oppholdsperioder() {
        var p1 = new OppholdPeriode(LocalDate.now(), LocalDate.now(), OppholdPeriode.Årsak.FERIE, OppholdPeriode.OppholdKilde.INNTEKTSMELDING);
        var p2 = new OppholdPeriode(LocalDate.now(), LocalDate.now(), OppholdPeriode.Årsak.FERIE, OppholdPeriode.OppholdKilde.SAKSBEHANDLER);
        var p3 = new OppholdPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1), OppholdPeriode.Årsak.FERIE, OppholdPeriode.OppholdKilde.SAKSBEHANDLER);
        var oppholdsperioder = Set.of(p1, p2, p3);
        var arbeidsforholdUttak = new ArbeidsforholdUttak(null, null, null, null, Set.of(), oppholdsperioder, null);

        assertThat(arbeidsforholdUttak.oppholdsperioder()).hasSize(2);

    }

}
