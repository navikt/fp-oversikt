package no.nav.foreldrepenger.oversikt.domene.fp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Uttak(@JsonProperty("dekningsgrad") Dekningsgrad dekningsgrad,
                    @JsonProperty("perioder") List<Uttaksperiode> perioder) {
}
