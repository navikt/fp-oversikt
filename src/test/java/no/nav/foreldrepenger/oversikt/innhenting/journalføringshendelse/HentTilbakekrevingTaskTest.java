package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving.Tilbakekreving;

class HentTilbakekrevingTaskTest {

    @Test
    void map() {
        var saksnummer = Saksnummer.dummy();
        var fraFptilbake = new Tilbakekreving(saksnummer.value(), new Tilbakekreving.Varsel(LocalDateTime.now(), true), true);
        var resultat = HentTilbakekrevingTask.map(fraFptilbake);

        assertThat(resultat.saksnummer()).isEqualTo(saksnummer);
        assertThat(resultat.harVerge()).isEqualTo(fraFptilbake.harVerge());
        assertThat(resultat.varsel().besvart()).isEqualTo(fraFptilbake.varsel().besvart());
        assertThat(resultat.varsel().utsendtTidspunkt()).isEqualTo(fraFptilbake.varsel().utsendtTidspunkt());
    }
}
