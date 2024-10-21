package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.common.innsyn.inntektsmelding.FpSakInntektsmeldingDto;

import org.junit.jupiter.api.Test;

class HentInntektsmeldingerTaskTest {

    @Test
    void map() {
        var fraFpsak = new FpSakInntektsmeldingDto(true, null, new BigDecimal(100), new BigDecimal(100), "Arbeidsgiveren", "12312312", "1",
            LocalDateTime.now().minusWeeks(1), LocalDate.now(), List.of(), List.of());
        var resultat = HentInntektsmeldingerTask.mapV2(fraFpsak);

        assertThat(resultat.journalpostId()).isEqualTo(fraFpsak.journalpostId());
        assertThat(resultat.inntektPrMnd()).isEqualTo(fraFpsak.inntektPrMnd());
        assertThat(resultat.arbeidsgiverNavn()).isEqualTo(fraFpsak.arbeidsgiverNavn());
        assertThat(resultat.mottattTidspunkt()).isEqualTo(fraFpsak.mottattTidspunkt());
    }
}
