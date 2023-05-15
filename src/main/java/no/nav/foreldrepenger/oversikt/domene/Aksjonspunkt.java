package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Aksjonspunkt(@JsonProperty("kode") String kode,
                           @JsonProperty("status") Status status,
                           @JsonProperty("venteÅrsak") String venteÅrsak,
                           @JsonProperty("opprettetTidspunkt") LocalDateTime opprettetTidspunkt) {

    public enum Status {
        UTFØRT,
        OPPRETTET,
    }
}
