package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public record TilbakekrevingV1(@JsonProperty("saksnummer") Saksnummer saksnummer,
                               @JsonProperty("varsel") Varsel varsel,
                               @JsonProperty("harVerge") boolean harVerge,
                               @JsonProperty("oppdatertTidspunkt") LocalDateTime oppdatertTidspunkt) implements Tilbakekreving {

    @Override
    public boolean trengerSvarFraBruker() {
        return varsel != null && !varsel.besvart && !harVerge;
    }

    @Override
    public LocalDate varselDato() {
        return trengerSvarFraBruker() ? varsel.utsendtTidspunkt().toLocalDate() : null;
    }

    public record Varsel(@JsonProperty("utsendtTidspunkt") LocalDateTime utsendtTidspunkt, @JsonProperty("besvart") boolean besvart) {

    }
}
