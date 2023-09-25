package no.nav.foreldrepenger.oversikt.oppgave;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
class OppgaveRepository {

    private EntityManager entityManager;

    @Inject
    OppgaveRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    OppgaveRepository() {
    }

    Oppgave hent(UUID id) {
        var entitet = hentOppgave(id);
        return map(entitet);
    }

    Set<Oppgave> hentFor(Saksnummer saksnummer) {
        return entityManager.createQuery("from oppgave where saksnummer=:saksnummer", OppgaveEntitet.class)
            .setParameter("saksnummer", saksnummer.value())
            .getResultList()
            .stream()
            .map(OppgaveRepository::map)
            .collect(Collectors.toSet());
    }

    void lagreStatus(UUID oppgaveId, Oppgave.Status status) {
        var entitet = hentOppgave(oppgaveId);
        entitet.setOpprettetTidspunkt(status.opprettetTidspunkt());
        entitet.setAvsluttetTidspunkt(status.avsluttetTidspunkt());
        entityManager.merge(entitet);
        entityManager.flush();
    }

    void opprett(Oppgave oppgave) {
        var entitet = new OppgaveEntitet(oppgave);
        entityManager.persist(entitet);
        entityManager.flush();
    }

    private static Oppgave map(OppgaveEntitet e) {
        var status = new Oppgave.Status(e.getOpprettetTidspunkt(), e.getAvsluttetTidspunkt());
        return new Oppgave(new Saksnummer(e.getSaksnummer()), e.getId(), e.getType(), status);
    }

    private OppgaveEntitet hentOppgave(UUID id) {
        var query = entityManager.createQuery("from oppgave where id=:id", OppgaveEntitet.class).setParameter("id", id);
        return hentEksaktResultat(query);
    }
}
