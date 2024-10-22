package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import no.nav.foreldrepenger.common.innsyn.inntektsmelding.NaturalytelseType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FpSakInntektsmeldingDto(Boolean erAktiv, BigDecimal stillingsprosent,
                               BigDecimal inntektPrMnd,
                               BigDecimal refusjonPrMnd,
                               String arbeidsgiverNavn,
                               String arbeidsgiverIdent,
                               String journalpostId,
                               LocalDateTime mottattTidspunkt,
                               LocalDate startDatoPermisjon,
                               List<NaturalYtelse> bortfalteNaturalytelser,
                               List<Refusjon> refusjonsperioder
){
    public record NaturalYtelse(
        LocalDate fomDato,
        LocalDate tomDato,
    BigDecimal beløpPerMnd,
    NaturalytelseType type
    ) {}

    public record Refusjon(
        BigDecimal refusjonsbeløpMnd,
        LocalDate fomDato
    ) {}
}

