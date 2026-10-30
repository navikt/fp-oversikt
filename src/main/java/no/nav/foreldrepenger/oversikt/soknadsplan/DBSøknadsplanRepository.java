package no.nav.foreldrepenger.oversikt.soknadsplan;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class DBSøknadsplanRepository implements SøknadsplanRepository {

    private EntityManager entityManager;

    DBSøknadsplanRepository() {
        // CDI
    }

    @Inject
    public DBSøknadsplanRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void lagre(Saksnummer saksnummer, Søknadsplan søknadsplan) {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE soknadsplan SET aktiv = false WHERE saksnummer = :saksnummer AND aktiv = true")
            .setParameter("saksnummer", saksnummer.value())
            .executeUpdate();
        entityManager.flush();
        entityManager.persist(new SøknadsplanEntitet(saksnummer, søknadsplan));
        entityManager.flush();
    }

    @Override
    @Transactional
    public Optional<Søknadsplan> hentAktivFor(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from soknadsplan where saksnummer = :saksnummer and aktiv = true", SøknadsplanEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query).map(SøknadsplanEntitet::map);
    }
}
