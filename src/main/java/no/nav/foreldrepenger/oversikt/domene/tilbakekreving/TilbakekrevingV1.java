package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public record TilbakekrevingV1(@JsonProperty("saksnummer") Saksnummer saksnummer,
                               @JsonProperty("varsel") Varsel varsel,
                               @JsonProperty("harVerge") boolean harVerge,
                               @JsonProperty("oppdatertTidspunkt") LocalDateTime oppdatertTidspunkt) implements Tilbakekreving {

    @Override
    public boolean varsleBrukerOmTilbakekreving() {
        if (varsel.besvart || harVerge) {
            return false;
        }
        return varsel.sendt;
    }

    public record Varsel(@JsonProperty("sendt") boolean sendt, @JsonProperty("besvart") boolean besvart) {

    }
}
