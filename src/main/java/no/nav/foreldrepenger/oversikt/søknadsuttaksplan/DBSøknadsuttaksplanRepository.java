package no.nav.foreldrepenger.oversikt.søknadsuttaksplan;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class DBSøknadsuttaksplanRepository implements SøknadsuttaksplanRepository {

    private EntityManager entityManager;

    DBSøknadsuttaksplanRepository() {
        // CDI
    }

    @Inject
    public DBSøknadsuttaksplanRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void lagre(Saksnummer saksnummer, Søknadsuttaksplan søknadsuttaksplan) {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE soknadsuttaksplan SET aktiv = false WHERE saksnummer = :saksnummer AND aktiv = true")
            .setParameter("saksnummer", saksnummer.value())
            .executeUpdate();
        entityManager.flush();
        entityManager.persist(new SøknadsuttaksplanEntitet(saksnummer, søknadsuttaksplan));
        entityManager.flush();
    }

    @Override
    @Transactional
    public Optional<Søknadsuttaksplan> hentAktivFor(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from soknadsuttaksplan where saksnummer = :saksnummer and aktiv = true", SøknadsuttaksplanEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query).map(SøknadsuttaksplanEntitet::map);
    }
}
