package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

class TilbakekrevingV1Test {

    @Test
    void trenger_svar() {
        var tk = new TilbakekrevingV1(Saksnummer.dummy(), new TilbakekrevingV1.Varsel(LocalDateTime.now(), false), false, LocalDateTime.now());
        assertThat(tk.trengerSvarFraBruker()).isTrue();
    }

    @Test
    void trenger_ikke_svar_hvis_besvart() {
        var tk = new TilbakekrevingV1(Saksnummer.dummy(), new TilbakekrevingV1.Varsel(LocalDateTime.now(), true), false, LocalDateTime.now());
        assertThat(tk.trengerSvarFraBruker()).isFalse();
    }

    @Test
    void trenger_ikke_svar_hvis_verge() {
        var tk = new TilbakekrevingV1(Saksnummer.dummy(), new TilbakekrevingV1.Varsel(LocalDateTime.now(), false), true, LocalDateTime.now());
        assertThat(tk.trengerSvarFraBruker()).isFalse();
    }

    @Test
    void trenger_ikke_svar_hvis_etter_tidsfrist() {
        var tk = new TilbakekrevingV1(Saksnummer.dummy(), new TilbakekrevingV1.Varsel(LocalDateTime.now().minusWeeks(4), false), false,
            LocalDateTime.now());
        assertThat(tk.trengerSvarFraBruker()).isFalse();
    }

    @Test
    void trenger_ikke_svar_hvis_ikke_varsel() {
        var tk = new TilbakekrevingV1(Saksnummer.dummy(), null, false, LocalDateTime.now());
        assertThat(tk.trengerSvarFraBruker()).isFalse();
    }
}
