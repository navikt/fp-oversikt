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
        var før = new TilbakekrevingV1(Saksnummer.dummy(), new TilbakekrevingV1.Varsel(true, true), true, LocalDateTime.now());
        repository.lagre(før);

        var tilbakekrevinger = repository.hentFor(Set.of(før.saksnummer()));
        assertThat(tilbakekrevinger).hasSize(1);

        var tilbakekreving = (TilbakekrevingV1) tilbakekrevinger.stream().findFirst().orElseThrow();
        assertThat(tilbakekreving.saksnummer()).isEqualTo(før.saksnummer());
        assertThat(tilbakekreving.harVerge()).isEqualTo(før.harVerge());
        assertThat(tilbakekreving.varsel().sendt()).isEqualTo(før.varsel().sendt());
        assertThat(tilbakekreving.varsel().besvart()).isEqualTo(før.varsel().besvart());
    }

}
