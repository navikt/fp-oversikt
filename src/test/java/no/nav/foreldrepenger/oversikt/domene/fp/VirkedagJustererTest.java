package no.nav.foreldrepenger.oversikt.domene.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs;

class VirkedagJustererTest {

    @Test
    void fom_lørdag_justeres_til_mandag() {
        var lørdag = LocalDate.of(2026, 4, 18); // lørdag
        assertThat(VirkedagJusterer.justerFom(lørdag)).isEqualTo(LocalDate.of(2026, 4, 20)); // mandag
    }

    @Test
    void fom_søndag_justeres_til_mandag() {
        var søndag = LocalDate.of(2026, 4, 19); // søndag
        assertThat(VirkedagJusterer.justerFom(søndag)).isEqualTo(LocalDate.of(2026, 4, 20)); // mandag
    }

    @Test
    void tom_lørdag_justeres_til_fredag() {
        var lørdag = LocalDate.of(2026, 4, 18); // lørdag
        assertThat(VirkedagJusterer.justerTom(lørdag)).isEqualTo(LocalDate.of(2026, 4, 17)); // fredag
    }

    @Test
    void tom_søndag_justeres_til_fredag() {
        var søndag = LocalDate.of(2026, 4, 19); // søndag
        assertThat(VirkedagJusterer.justerTom(søndag)).isEqualTo(LocalDate.of(2026, 4, 17)); // fredag
    }

    @Test
    void virkedager_forblir_uendret() {
        var mandag = LocalDate.of(2026, 4, 20);
        var fredag = LocalDate.of(2026, 4, 24);
        assertThat(VirkedagJusterer.justerFom(mandag)).isEqualTo(mandag);
        assertThat(VirkedagJusterer.justerTom(fredag)).isEqualTo(fredag);
    }

    @Test
    void eøs_perioder_justeres_korrekt() {
        var lørdag = LocalDate.of(2026, 4, 18);
        var søndag = LocalDate.of(2026, 4, 19);
        var mandag = LocalDate.of(2026, 4, 20);
        var fredag = LocalDate.of(2026, 4, 24);

        var perioder = List.of(lagEøsPeriode(mandag, fredag), lagEøsPeriode(lørdag, søndag));

        var justert = VirkedagJusterer.justerEøsPerioder(perioder);

        assertThat(justert).hasSize(1);
        assertThat(justert.getFirst().fom()).isEqualTo(mandag);
        assertThat(justert.getFirst().tom()).isEqualTo(fredag);
    }

    @Test
    void eøs_perioder_lørdag_til_fredag_justeres() {
        var lørdag = LocalDate.of(2026, 4, 18);
        var fredag = LocalDate.of(2026, 4, 24);

        var perioder = List.of(lagEøsPeriode(lørdag, fredag));

        var justert = VirkedagJusterer.justerEøsPerioder(perioder);

        assertThat(justert).hasSize(1);
        assertThat(justert.getFirst().fom()).isEqualTo(LocalDate.of(2026, 4, 20)); // mandag
        assertThat(justert.getFirst().tom()).isEqualTo(fredag);
    }

    private static UttakPeriodeAnnenpartEøs lagEøsPeriode(LocalDate fom, LocalDate tom) {
        return new UttakPeriodeAnnenpartEøs(fom, tom, KontoType.FORELDREPENGER, new UttakPeriodeAnnenpartEøs.Trekkdager(BigDecimal.TEN));
    }
}
