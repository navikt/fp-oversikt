package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.Beløp;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.Inntektsmelding;

class HentInntektsmeldingerTaskTest {

    @Test
    void map() {
        var fraFpsak = new Inntektsmelding(true, new BigDecimal(100), new BigDecimal(100), "Arbeidsgiveren", "1", "Sjef sjefsen", "11223344",
            LocalDateTime.now().minusWeeks(1), LocalDateTime.now().minusWeeks(1), LocalDate.now(), List.of(), List.of());
        var resultat = HentInntektsmeldingerTask.map(fraFpsak);

        assertThat(resultat.innsendingstidspunkt()).isEqualTo(fraFpsak.innsendingstidspunkt());
        assertThat(resultat.journalpostId()).isEqualTo(fraFpsak.journalpostId());
        assertThat(resultat.inntekt()).isEqualTo(new Beløp(fraFpsak.inntektPrMnd()));
        assertThat(resultat.arbeidsgiver().identifikator()).isEqualTo(fraFpsak.arbeidsgiverNavn());
        assertThat(resultat.mottattTidspunkt()).isEqualTo(fraFpsak.mottattTidspunkt());
    }
}
