package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = InntektsmeldingV1.class, name = "1")
})
public interface Inntektsmelding {
    String journalpostId();
    Arbeidsgiver arbeidsgiver();
    LocalDateTime innsendingstidspunkt();
}
