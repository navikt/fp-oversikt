package no.nav.foreldrepenger.oversikt.domene.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

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
}
