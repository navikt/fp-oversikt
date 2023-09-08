package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = InntektsmeldingV1.class, name = "1")
})
public interface Inntektsmelding {
}
