package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.JpaExtension;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ExtendWith(JpaExtension.class)
class DBTilbakekrevingRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBTilbakekrevingRepository(entityManager);
        var saksnummer = Saksnummer.dummy();
        var utsendtTidspunkt = LocalDateTime.now();
        var før = new TilbakekrevingV1(saksnummer, new TilbakekrevingV1.Varsel(utsendtTidspunkt, true), true, LocalDateTime.now());
        repository.lagre(før);

        var tilbakekrevinger = repository.hentFor(Set.of(saksnummer));
        assertThat(tilbakekrevinger).hasSize(1);

        var tilbakekreving = (TilbakekrevingV1) tilbakekrevinger.stream().findFirst().orElseThrow();
        assertThat(tilbakekreving.saksnummer()).isEqualTo(før.saksnummer());
        assertThat(tilbakekreving.harVerge()).isEqualTo(før.harVerge());
        assertThat(tilbakekreving.varsel().utsendtTidspunkt()).isEqualTo(før.varsel().utsendtTidspunkt());
        assertThat(tilbakekreving.varsel().besvart()).isEqualTo(før.varsel().besvart());

        repository.slett(saksnummer);
        assertThat(repository.hentFor(Set.of(saksnummer))).isEmpty();
    }

}
