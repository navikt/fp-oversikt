package no.nav.foreldrepenger.oversikt.domene.svp;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OppholdPeriode(@JsonProperty("fom") LocalDate fom,
                             @JsonProperty("tom") LocalDate tom,
                             @JsonProperty("årsak") Årsak årsak) {
    public enum Årsak {
        FERIE,
        SYKEPENGER
    }
}
