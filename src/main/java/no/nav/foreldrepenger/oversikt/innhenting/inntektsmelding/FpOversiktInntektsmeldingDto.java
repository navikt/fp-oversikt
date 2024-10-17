package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FpOversiktInntektsmeldingDto(
    int versjon,
    boolean erAktiv,
    BigDecimal stillingsprosent,
    BigDecimal inntektPrMnd,
    BigDecimal refusjonPrMnd,
    String arbeidsgiverNavn,
    String journalpostId,
    LocalDateTime mottattTidspunkt,
    LocalDate startDatoPermisjon,
    List<NaturalYtelse> bortfalteNaturalytelser,
    List<Refusjon> refusjonsperioder
) {
    public record NaturalYtelse(LocalDate fomDato, LocalDate tomDato, BigDecimal beløpPerMnd, String type) {
    }

    public record Refusjon(LocalDate fomDato, BigDecimal refusjonsbeløpMnd) {
    }
}
