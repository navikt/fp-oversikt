package no.nav.foreldrepenger.oversikt.domene.svp;


import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.Prosent;

public record TilretteleggingPeriode(@JsonProperty("fom") LocalDate fom,
                                     @JsonProperty("type") TilretteleggingType type,
                                     @JsonProperty("arbeidstidprosent") Prosent arbeidstidprosent) {

}
