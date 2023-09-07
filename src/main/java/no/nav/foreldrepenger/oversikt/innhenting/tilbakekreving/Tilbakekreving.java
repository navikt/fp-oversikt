package no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving;

public record Tilbakekreving(String saksnummer, Varsel varsel, boolean harVerge) {

    public record Varsel(boolean sendt, boolean besvart) {

    }
}
