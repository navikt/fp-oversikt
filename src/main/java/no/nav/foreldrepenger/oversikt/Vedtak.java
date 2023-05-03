package no.nav.foreldrepenger.oversikt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "versjon")
@JsonSubTypes({
    @JsonSubTypes.Type(value = VedtakV0.class, name = "0"),
    //@JsonSubTypes.Type(value = VedtakV1.class, name = "1")
})
public interface Vedtak {
    String saksnummer();

    String aktørId();
}
