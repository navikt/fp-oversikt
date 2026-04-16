package no.nav.foreldrepenger.oversikt.domene.fp;

import static no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak.MOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriode;
import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeResultat;

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
    void periode_kun_i_helg_filtreres_ut() {
        var lørdag = LocalDate.of(2026, 4, 18);
        var søndag = LocalDate.of(2026, 4, 19);
        var perioder = List.of(lagUttakPeriode(lørdag, søndag));

        var justert = VirkedagJusterer.justerUttakPerioder(perioder);

        assertThat(justert).isEmpty();
    }

    @Test
    void periode_søndag_til_søndag_filtreres_ut() {
        var søndag = LocalDate.of(2026, 4, 19);
        var perioder = List.of(lagUttakPeriode(søndag, søndag));

        var justert = VirkedagJusterer.justerUttakPerioder(perioder);

        assertThat(justert).isEmpty();
    }

    @Test
    void tom_før_fom_etter_justering_filtreres_ut() {
        // lørdag til lørdag: fom→man, tom→fre, man > fre → filtrer ut
        var lørdag = LocalDate.of(2026, 4, 18);
        var perioder = List.of(lagUttakPeriode(lørdag, lørdag));

        var justert = VirkedagJusterer.justerUttakPerioder(perioder);

        assertThat(justert).isEmpty();
    }

    @Test
    void periode_fredag_til_søndag_justeres_til_fredag_til_fredag() {
        var fredag = LocalDate.of(2026, 4, 17);
        var søndag = LocalDate.of(2026, 4, 19);
        var perioder = List.of(lagUttakPeriode(fredag, søndag));

        var justert = VirkedagJusterer.justerUttakPerioder(perioder);

        assertThat(justert).hasSize(1);
        assertThat(justert.getFirst().fom()).isEqualTo(fredag);
        assertThat(justert.getFirst().tom()).isEqualTo(fredag);
    }

    @Test
    void periode_lørdag_til_mandag_justeres_til_mandag_til_mandag() {
        var lørdag = LocalDate.of(2026, 4, 18);
        var mandag = LocalDate.of(2026, 4, 20);
        var perioder = List.of(lagUttakPeriode(lørdag, mandag));

        var justert = VirkedagJusterer.justerUttakPerioder(perioder);

        assertThat(justert).hasSize(1);
        assertThat(justert.getFirst().fom()).isEqualTo(mandag);
        assertThat(justert.getFirst().tom()).isEqualTo(mandag);
    }

    @Test
    void vanlig_virkedagsperiode_forblir_uendret() {
        var mandag = LocalDate.of(2026, 4, 20);
        var fredag = LocalDate.of(2026, 4, 24);
        var perioder = List.of(lagUttakPeriode(mandag, fredag));

        var justert = VirkedagJusterer.justerUttakPerioder(perioder);

        assertThat(justert).hasSize(1);
        assertThat(justert.getFirst().fom()).isEqualTo(mandag);
        assertThat(justert.getFirst().tom()).isEqualTo(fredag);
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

    @Test
    void flere_perioder_justeres_og_filtreres_korrekt() {
        var mandag = LocalDate.of(2026, 4, 20);
        var fredag = LocalDate.of(2026, 4, 24);
        var lørdag = LocalDate.of(2026, 4, 25);
        var søndag = LocalDate.of(2026, 4, 26);

        var perioder = List.of(lagUttakPeriode(mandag, fredag), lagUttakPeriode(lørdag, søndag), lagUttakPeriode(mandag, søndag));

        var justert = VirkedagJusterer.justerUttakPerioder(perioder);

        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).fom()).isEqualTo(mandag);
        assertThat(justert.get(0).tom()).isEqualTo(fredag);
        assertThat(justert.get(1).fom()).isEqualTo(mandag);
        assertThat(justert.get(1).tom()).isEqualTo(fredag);
    }

    private static UttakPeriode lagUttakPeriode(LocalDate fom, LocalDate tom) {
        var resultat = new UttakPeriodeResultat(true, false, true, UttakPeriodeResultat.UttakPeriodeResultatÅrsak.ANNET);
        return new UttakPeriode(fom, tom, KontoType.FORELDREPENGER, resultat, null, null, null, null, null, null, false, MOR);
    }

    private static UttakPeriodeAnnenpartEøs lagEøsPeriode(LocalDate fom, LocalDate tom) {
        return new UttakPeriodeAnnenpartEøs(fom, tom, KontoType.FORELDREPENGER, new UttakPeriodeAnnenpartEøs.Trekkdager(BigDecimal.TEN));
    }
}
