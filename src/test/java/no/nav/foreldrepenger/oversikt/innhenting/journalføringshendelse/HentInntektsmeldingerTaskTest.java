package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.common.innsyn.inntektsmelding.NaturalytelseType;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.FpSakInntektsmeldingDto;

import no.nav.vedtak.konfig.Tid;

import org.junit.jupiter.api.Test;

class HentInntektsmeldingerTaskTest {

    @Test
    void map() {
        var bortfalteNaturalytelse1 = new FpSakInntektsmeldingDto.NaturalYtelse(LocalDate.now(), Tid.TIDENES_ENDE, new BigDecimal(1000), NaturalytelseType.BIL);
        var bortfalteNaturalytelse2 = new FpSakInntektsmeldingDto.NaturalYtelse(LocalDate.now(), Tid.TIDENES_ENDE, new BigDecimal(5000), NaturalytelseType.KOST_DØGN);

        var refusjon1 = new FpSakInntektsmeldingDto.Refusjon(new BigDecimal(500), LocalDate.now());

        var fraFpsak = new FpSakInntektsmeldingDto(true, null, new BigDecimal(100), new BigDecimal(100), "Arbeidsgiveren", "12312312", "1",
            LocalDateTime.now().minusWeeks(1), LocalDate.now(), List.of(bortfalteNaturalytelse1, bortfalteNaturalytelse2), List.of(refusjon1));
        var resultat = HentInntektsmeldingerTask.mapV2(fraFpsak);

        assertThat(resultat.journalpostId()).isEqualTo(fraFpsak.journalpostId());
        assertThat(resultat.inntektPrMnd()).isEqualTo(fraFpsak.inntektPrMnd());
        assertThat(resultat.arbeidsgiverNavn()).isEqualTo(fraFpsak.arbeidsgiverNavn());
        assertThat(resultat.mottattTidspunkt()).isEqualTo(fraFpsak.mottattTidspunkt());
        assertThat(resultat.bortfalteNaturalytelser().get(0).type()).isEqualTo("BIL");
        assertThat(resultat.bortfalteNaturalytelser().get(0).beløpPerMnd()).isEqualTo(bortfalteNaturalytelse1.beløpPerMnd());
        assertThat(resultat.bortfalteNaturalytelser().get(0).fomDato()).isEqualTo(bortfalteNaturalytelse1.fomDato());
        assertThat(resultat.bortfalteNaturalytelser().get(0).tomDato()).isEqualTo(bortfalteNaturalytelse1.tomDato());
        assertThat(resultat.bortfalteNaturalytelser().get(1).type()).isEqualTo("KOST_DØGN");
    }
}
