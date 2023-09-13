package no.nav.foreldrepenger.oversikt.oppgave;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;
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

        var før = new Oppgave(Saksnummer.dummy(), UUID.randomUUID(), OppgaveType.SVAR_TILBAKEKREVING,
            new Oppgave.Status(LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(5)),
            new Oppgave.StatusDittNav(LocalDateTime.now(), LocalDateTime.now().plusDays(5)));
        repository.opprett(Set.of(før));

        var etter = repository.hentFor(før.saksnummer());
        assertThat(etter).hasSize(1).first().extracting(oppgave -> oppgave).isEqualTo(før);
    }

    @Test
    void lagrerStatus(EntityManager entityManager) {
        var repository = new OppgaveRepository(entityManager);

        var oppgave = new Oppgave(Saksnummer.dummy(), UUID.randomUUID(), OppgaveType.SVAR_TILBAKEKREVING,
            new Oppgave.Status(LocalDateTime.now().minusDays(10), null), null);
        repository.opprett(Set.of(oppgave));
        var nyStatus = new Oppgave.Status(LocalDateTime.now(), LocalDateTime.now().plusWeeks(1));
        repository.lagreStatus(oppgave.id(), nyStatus);
        var nyStatusDittNav = new Oppgave.StatusDittNav(LocalDateTime.now().plusWeeks(12), LocalDateTime.now().plusWeeks(15));
        repository.lagreStatusDittNav(oppgave.id(), nyStatusDittNav);

        var etter = repository.hentFor(oppgave.saksnummer()).stream().findFirst().orElseThrow();
        assertThat(etter.status()).isEqualTo(nyStatus);
        assertThat(etter.dittNavStatus()).isEqualTo(nyStatusDittNav);
    }
}

