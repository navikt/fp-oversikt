package no.nav.foreldrepenger.oversikt.domene.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.innsyn.BrukerRolleSak;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Prosent;

class FpVedtakTest {

    @Test
    void skal_slå_sammen_like_perioder() {
        var uttaksperiode1 = lagPeriode(LocalDate.of(2024, 9, 2), LocalDate.of(2024, 9, 3), Konto.MØDREKVOTE);
        var uttaksperiode2 = lagPeriode(LocalDate.of(2024, 9, 4), LocalDate.of(2024, 9, 6), Konto.MØDREKVOTE);
        var uttaksperiode3 = lagPeriode(LocalDate.of(2024, 9, 9), LocalDate.of(2024, 9, 15), Konto.MØDREKVOTE);
        var uttaksperiode4 = lagPeriode(LocalDate.of(2024, 9, 16), LocalDate.of(2024, 9, 17), Konto.MØDREKVOTE);
        var uttaksperiode5 = lagPeriode(LocalDate.of(2024, 9, 18), LocalDate.of(2024, 9, 20), Konto.FELLESPERIODE);
        var uttaksperiode6 = lagPeriode(LocalDate.of(2024, 9, 21), LocalDate.of(2024, 9, 27), Konto.MØDREKVOTE);
        var vedtak = new FpVedtak(LocalDateTime.now(),
            List.of(uttaksperiode1, uttaksperiode2, uttaksperiode3, uttaksperiode4, uttaksperiode5, uttaksperiode6), Dekningsgrad.HUNDRE, null);

        var resultat = vedtak.tilDto(BrukerRolleSak.MOR).perioder();
        assertThat(resultat).hasSize(3);
        assertThat(resultat.getFirst().fom()).isEqualTo(uttaksperiode1.fom());
        assertThat(resultat.getFirst().tom()).isEqualTo(uttaksperiode4.tom());
    }

    private Uttaksperiode lagPeriode(LocalDate fom, LocalDate tom, Konto konto) {
        return new Uttaksperiode(fom, tom, null, null, null, null, false, MorsAktivitet.ARBEID,
            new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET, Uttaksperiode.Resultat.Årsak.ANNET, Set.of(
                new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null), konto,
                    new Trekkdager(5), new Prosent(100))), false));
    }

}
