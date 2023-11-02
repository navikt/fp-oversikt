package no.nav.foreldrepenger.oversikt.oppgave;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.JpaExtension;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ExtendWith(JpaExtension.class)
class OppgaveRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new OppgaveRepository(entityManager);

        var før = new Oppgave(Saksnummer.dummy(), UUID.randomUUID(), OppgaveType.LAST_OPP_MANGLENDE_VEDLEGG,
            new Oppgave.Status(LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(5)));
        repository.opprett(før);

        var etter = repository.hentFor(før.saksnummer());
        assertThat(etter).hasSize(1).first().extracting(oppgave -> oppgave).isEqualTo(før);
    }

    @Test
    void lagrerStatus(EntityManager entityManager) {
        var repository = new OppgaveRepository(entityManager);

        var oppgave = new Oppgave(Saksnummer.dummy(), UUID.randomUUID(), OppgaveType.LAST_OPP_MANGLENDE_VEDLEGG,
            new Oppgave.Status(LocalDateTime.now().minusDays(10), null));
        repository.opprett(oppgave);
        var nyStatus = new Oppgave.Status(LocalDateTime.now(), LocalDateTime.now().plusWeeks(1));
        repository.lagreStatus(oppgave.id(), nyStatus);

        var etter = repository.hentFor(oppgave.saksnummer()).stream().findFirst().orElseThrow();
        assertThat(etter.status()).isEqualTo(nyStatus);
    }
}

