package no.nav.foreldrepenger.oversikt.søknad;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class DBSøknadRepository implements SøknadRepository {

    private EntityManager entityManager;

    DBSøknadRepository() {
        // CDI
    }

    @Inject
    public DBSøknadRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void lagre(Saksnummer saksnummer, Søknad søknad) {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE soknad SET aktiv = false WHERE saksnummer = :saksnummer AND aktiv = true")
            .setParameter("saksnummer", saksnummer.value())
            .executeUpdate();
        entityManager.flush();
        entityManager.persist(new SøknadEntitet(saksnummer, søknad));
        entityManager.flush();
    }

    @Override
    @Transactional
    public Optional<Søknad> hentAktivFor(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from soknad where saksnummer = :saksnummer and aktiv = true", SøknadEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query).map(SøknadEntitet::map);
    }
}
