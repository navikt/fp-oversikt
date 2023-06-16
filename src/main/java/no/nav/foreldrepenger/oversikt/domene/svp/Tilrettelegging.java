package no.nav.foreldrepenger.oversikt.domene.svp;

import static no.nav.foreldrepenger.oversikt.domene.NullUtil.nullSafe;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Tilrettelegging(@JsonProperty("aktivitet") Aktivitet aktivitet,
                              @JsonProperty("behovFom") LocalDate behovFom,
                              @JsonProperty("risikoFaktorer") String risikoFaktorer,
                              @JsonProperty("tiltak") String tiltak,
                              @JsonProperty("perioder") Set<TilretteleggingPeriode> perioder,
                              @JsonProperty("oppholdsperioder") Set<OppholdPeriode> oppholdsperioder
                             ) {

    public Set<TilretteleggingPeriode> perioder() {
        return nullSafe(perioder);
    }

    @Override
    public Set<OppholdPeriode> oppholdsperioder() {
        return nullSafe(oppholdsperioder);
    }
}
