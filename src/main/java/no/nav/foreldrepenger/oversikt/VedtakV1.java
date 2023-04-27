package no.nav.foreldrepenger.oversikt;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VedtakV1(@JsonProperty("opprettet") LocalDateTime opprettet,
                       @JsonProperty("enhetNavnBlala") String behandlendeEnhetNavn) implements Vedtak {

}
