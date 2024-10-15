package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InntektsmeldingV2(
    @JsonProperty("inntektPrMnd") BigDecimal inntektPrMnd,
    @JsonProperty("refusjonPrMnd") BigDecimal refusjonPrMnd,
    @JsonProperty("arbeidsgiver") Arbeidsgiver arbeidsgiver,
    @JsonProperty("journalpostId") String journalpostId,
    @JsonProperty("kontaktpersonNavn") String kontaktpersonNavn,
    @JsonProperty("kontaktpersonNummer") String kontaktpersonNummer,
    @JsonProperty("innsendingstidspunkt") LocalDateTime innsendingstidspunkt,
    @JsonProperty("mottattTidspunkt") LocalDateTime mottattTidspunkt,
    @JsonProperty("startDatoPermisjon") LocalDate startDatoPermisjon,
    @JsonProperty("aktiveNaturalytelser") List<NaturalYtelse> aktiveNaturalytelser,
    @JsonProperty("refusjonsperioder") List<Refusjon> refusjonsperioder
) implements Inntektsmelding {
    public record NaturalYtelse(
        @JsonProperty("fomDato") LocalDate fomDato,
        @JsonProperty("tomDato") LocalDate tomDato,
        @JsonProperty("beloepPerMnd") BigDecimal beloepPerMnd,
        @JsonProperty("type") String type
    ) {}

    public record Refusjon(
        @JsonProperty("fomDato") LocalDate fomDato,
        @JsonProperty("refusjonsbeløpMnd") BigDecimal refusjonsbeløpMnd
    ) {}
}

