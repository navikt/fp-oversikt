package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Beløp;

public record InntektsmeldingV1(@JsonProperty("journalpostId") String journalpostId,
                                @JsonProperty("arbeidsgiver") Arbeidsgiver arbeidsgiver,
                                @JsonProperty("innsendingstidspunkt") LocalDateTime innsendingstidspunkt,
                                @JsonProperty("inntekt") Beløp inntekt) implements Inntektsmelding {
}
