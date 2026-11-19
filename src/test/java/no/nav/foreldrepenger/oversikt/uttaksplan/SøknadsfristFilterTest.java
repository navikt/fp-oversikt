package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

class SøknadsfristFilterTest {

    private static final LocalDate I_DAG = LocalDate.of(2026, 11, 18);
    private static final LocalDate GRENSE = LocalDate.of(2026, 7, 18);

    @Test
    void tidligsteUttaksdatoErFireMånederFørIDag() {
        assertThat(SøknadsfristFilter.tidligsteUttaksdato(I_DAG)).isEqualTo(GRENSE);
        // Lørdag, ingen justering til mandag
        assertThat(SøknadsfristFilter.tidligsteUttaksdato(LocalDate.of(2026, 10, 31))).isEqualTo(LocalDate.of(2026, 6, 30));
        // Dagen finnes ikke i februar
        assertThat(SøknadsfristFilter.tidligsteUttaksdato(LocalDate.of(2027, 6, 30))).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void fjernerUttakSomSlutterFørGrensen() {
        var periode = uttak(LocalDate.of(2026, 5, 20), GRENSE.minusDays(1));

        assertThat(SøknadsfristFilter.fjernUttakFørFrist(List.of(periode), I_DAG)).isEmpty();
    }

    @Test
    void beholderUttakSomSlutterPåGrensen() {
        var periode = uttak(LocalDate.of(2026, 6, 22), GRENSE);

        assertThat(SøknadsfristFilter.fjernUttakFørFrist(List.of(periode), I_DAG)).containsExactly(periode);
    }

    @Test
    void beholderHeleUttakSomKrysserGrensen() {
        var periode = uttak(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 24));

        assertThat(SøknadsfristFilter.fjernUttakFørFrist(List.of(periode), I_DAG)).containsExactly(periode);
    }

    @Test
    void beholderUttakEtterGrensenUendret() {
        var periode = uttak(GRENSE, LocalDate.of(2026, 8, 14));

        assertThat(SøknadsfristFilter.fjernUttakFørFrist(List.of(periode), I_DAG)).containsExactly(periode);
    }

    @Test
    void beholderUtsettelserFørGrensen() {
        var utsettelse = new Planperiode(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 2),
            new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, FellesUttaksplanDto.UtsettelseÅrsak.ARBEID, null, null,
                null, null, false, null));

        assertThat(SøknadsfristFilter.fjernUttakFørFrist(List.of(utsettelse), I_DAG)).containsExactly(utsettelse);
    }

    private static Planperiode uttak(LocalDate fom, LocalDate tom) {
        return new Planperiode(fom, tom, new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR,
            no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FEDREKVOTE, null, null, null, null, null, false, null));
    }
}
