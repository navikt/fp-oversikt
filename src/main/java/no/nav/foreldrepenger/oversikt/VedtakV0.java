package no.nav.foreldrepenger.oversikt;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VedtakV0(@JsonProperty("opprettet") LocalDateTime opprettet,
                       @JsonProperty("uuid") UUID uuid,
                       @JsonProperty("enhetNavn") String behandlendeEnhetNavn) implements Vedtak {

}
