package no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving;

import java.time.LocalDateTime;

public record Tilbakekreving(String saksnummer, Varsel varsel, boolean harVerge) {

    public record Varsel(LocalDateTime utsendtTidspunkt, boolean besvart) {

    }
}
