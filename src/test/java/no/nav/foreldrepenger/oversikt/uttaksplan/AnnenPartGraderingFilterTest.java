package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.Aktivitet;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.Arbeidsgiver;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.Arbeidstidprosent;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.Gradering;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.Rolle;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.UttakDto;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

class AnnenPartGraderingFilterTest {

    @Test
    void skalFjerneAktivitetsopplysninger() {
        var aktivitet = new Aktivitet(Aktivitet.AktivitetType.ORDINÆRT_ARBEID,
            new Arbeidsgiver("999999999", Arbeidsgiver.ArbeidsgiverType.ORGANISASJON), "Arbeidsgiver AS");
        var gradering = new Gradering(new Arbeidstidprosent(BigDecimal.valueOf(40)), aktivitet);
        var uttak = new UttakDto(Rolle.FAR_MEDMOR, null, null, null, gradering, null, null, false, null);
        var periode = new Planperiode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), uttak);

        var filtrert = AnnenPartGraderingFilter.fjernArbeidsgivere(List.of(periode));

        assertThat(filtrert).singleElement().satisfies(p -> {
            assertThat(p.uttak().gradering().arbeidstidprosent().value()).isEqualByComparingTo(BigDecimal.valueOf(40));
            assertThat(p.uttak().gradering().aktivitet()).isNull();
        });
    }
}
