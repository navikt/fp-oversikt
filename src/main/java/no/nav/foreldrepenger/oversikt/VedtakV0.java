package no.nav.foreldrepenger.oversikt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VedtakV0(@JsonProperty("saksnummer") String saksnummer,
                       @JsonProperty("status") String status,
                       @JsonProperty("ytelseType") String ytelseType,
                       @JsonProperty("aktørId") String aktørId) implements Vedtak {

}
