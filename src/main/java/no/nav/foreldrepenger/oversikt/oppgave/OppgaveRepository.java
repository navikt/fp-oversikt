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

    Set<Oppgave> hentFor(Saksnummer saksnummer) {
        return entityManager.createQuery("from oppgave where saksnummer=:saksnummer", OppgaveEntitet.class)
            .setParameter("saksnummer", saksnummer.value())
            .getResultList()
            .stream()
            .map(e -> {
                var status = new Oppgave.Status(e.getOpprettetTidspunkt(), e.getAvsluttetTidspunkt());
                var dittNavStatus = new Oppgave.StatusDittNav(e.getOpprettetTidspunktDittNav(), e.getAvsluttetTidspunktDittNav());
                return new Oppgave(new Saksnummer(e.getSaksnummer()), e.getId(), e.getType(), status, dittNavStatus);
            })
            .collect(Collectors.toSet());
    }

    void lagreStatus(UUID oppgaveId, Oppgave.Status status) {
        var entitet = hentOppgave(oppgaveId);
        entitet.setOpprettetTidspunkt(status.opprettetTidspunkt());
        entitet.setAvsluttetTidspunkt(status.avsluttetTidspunkt());
        entityManager.merge(entitet);
        entityManager.flush();
    }

    void lagreStatusDittNav(UUID oppgaveId, Oppgave.StatusDittNav status) {
        var entitet = hentOppgave(oppgaveId);
        entitet.setOpprettetTidspunktDittNav(status.opprettetTidspunkt());
        entitet.setAvsluttetTidspunktDittNav(status.avsluttetTidspunkt());
        entityManager.merge(entitet);
        entityManager.flush();
    }

    private OppgaveEntitet hentOppgave(UUID id) {
        var query = entityManager.createQuery("from oppgave where id=:id", OppgaveEntitet.class).setParameter("id", id);
        return hentEksaktResultat(query);
    }

    void opprett(Set<Oppgave> oppgaver) {
        for (var oppgave : oppgaver) {
            var entitet = new OppgaveEntitet(oppgave);
            entityManager.persist(entitet);
        }
        entityManager.flush();
    }
}
