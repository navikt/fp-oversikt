package no.nav.foreldrepenger.oversikt.domene.svp;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.Prosent;

public record SvpPeriode(@JsonProperty("fom") LocalDate fom,
                         @JsonProperty("tom") LocalDate tom,
                         @JsonProperty("tilretteleggingType") TilretteleggingType tilretteleggingType,
                         @JsonProperty("arbeidstidprosent") Prosent arbeidstidprosent,
                         @JsonProperty("utbetalingsgrad") Prosent utbetalingsgrad,
                         @JsonProperty("resultatÅrsak") ResultatÅrsak resultatÅrsak) {
}
