package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.kontrakter.fpoversikt.inntektsmelding.NaturalytelseType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InntektsmeldingV2(
    @JsonProperty("erAktiv") boolean erAktiv,
    @JsonProperty("stillingsprosent") BigDecimal stillingsprosent,
    @JsonProperty("inntektPrMnd") BigDecimal inntektPrMnd,
    @JsonProperty("refusjonPrMnd") BigDecimal refusjonPrMnd,
    @JsonProperty("arbeidsgiverNavn") String arbeidsgiverNavn,
    @JsonProperty("arbeidsgiverIdent") String arbeidsgiverIdent,
    @JsonProperty("journalpostId") String journalpostId,
    @JsonProperty("mottattTidspunkt") LocalDateTime mottattTidspunkt,
    @JsonProperty("startDatoPermisjon") LocalDate startDatoPermisjon,
    @JsonProperty("bortfalteNaturalytelser") List<NaturalYtelse> bortfalteNaturalytelser,
    @JsonProperty("refusjonsperioder") List<Refusjon> refusjonsperioder
) implements Inntektsmelding {
    public record NaturalYtelse(
        @JsonProperty("fomDato") LocalDate fomDato,
        @JsonProperty("tomDato") LocalDate tomDato,
        @JsonProperty("beløpPerMnd") BigDecimal beløpPerMnd,
        @JsonProperty("type") NaturalytelseType type
    ) {}

    public record Refusjon(
        @JsonProperty("fomDato") LocalDate fomDato,
        @JsonProperty("refusjonsbeløpMnd") BigDecimal refusjonsbeløpMnd
    ) {}
}

