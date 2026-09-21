package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class DBAnnenPartUttaksplanRepository implements AnnenPartUttaksplanRepository {

    private EntityManager entityManager;

    @Inject
    public DBAnnenPartUttaksplanRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    DBAnnenPartUttaksplanRepository() {
        // CDI
    }

    @Override
    public void lagre(Saksnummer saksnummer, AnnenPartUttaksplan uttaksplan) {
        låsSaksnummer(saksnummer);
        var eksisterende = hentEntitetForOppdatering(saksnummer);
        if (eksisterende.isEmpty()) {
            entityManager.persist(new AnnenPartUttaksplanEntitet(saksnummer, uttaksplan));
        } else if (!eksisterende.get().map().mottattTidspunkt().isAfter(uttaksplan.mottattTidspunkt())) {
            eksisterende.get().setJson(uttaksplan);
            entityManager.merge(eksisterende.get());
        }
        entityManager.flush();
    }

    private void låsSaksnummer(Saksnummer saksnummer) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtext(cast(?1 as text)))", Void.class)
            .setParameter(1, saksnummer.value())
            .getSingleResult();
    }

    @Override
    public Optional<AnnenPartUttaksplan> hentFor(Saksnummer saksnummer) {
        return hentEntitet(saksnummer).map(AnnenPartUttaksplanEntitet::map);
    }

    private Optional<AnnenPartUttaksplanEntitet> hentEntitet(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from annen_part_uttaksplan where saksnummer = :saksnummer", AnnenPartUttaksplanEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query);
    }

    private Optional<AnnenPartUttaksplanEntitet> hentEntitetForOppdatering(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from annen_part_uttaksplan where saksnummer = :saksnummer", AnnenPartUttaksplanEntitet.class)
            .setParameter("saksnummer", saksnummer.value())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return hentUniktResultat(query);
    }
}
