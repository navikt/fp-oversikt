package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@ApplicationScoped
public class DBSakRepository implements SakRepository {

    private final EntityManager entityManager;

    @Inject
    public DBSakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lagre(Sak sak) {
        var eksisterendeSak = hentSak(sak.saksnummer());
        if (eksisterendeSak.isEmpty()) {
            entityManager.persist(new SakEntitet(sak));
        } else {
            eksisterendeSak.get().setJson(sak);
            entityManager.merge(eksisterendeSak.get());
        }
        entityManager.flush();
    }

    private Optional<SakEntitet> hentSak(Saksnummer saksnummer) {
        var query = hentSakQuery(saksnummer);
        return hentUniktResultat(query);
    }

    private TypedQuery<SakEntitet> hentSakQuery(Saksnummer saksnummer) {
        return entityManager.createQuery("from sak where saksnummer =:saksnummer", SakEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
    }

    @Override
    public List<Sak> hentFor(AktørId aktørId) {
        var query = entityManager.createNativeQuery("select * from sak where json->>'aktørId' = :aktørId", SakEntitet.class);
        query.setParameter("aktørId", aktørId.value());
        return ((List<SakEntitet>) query.getResultList())
            .stream()
            .map(SakEntitet::map)
            .toList();
    }

    @Override
    public boolean erSakKobletTilAktør(Saksnummer saksnummer, AktørId aktørId) {
        var nativeQuery = entityManager.createNativeQuery("select count(1) from sak where json->>'aktørId' = :aktørId and saksnummer =:saksnummer", Integer.class);
        nativeQuery.setParameter("aktørId", aktørId.value());
        nativeQuery.setParameter("saksnummer", saksnummer.value());
        var rader = (Integer) nativeQuery.getSingleResult();
        return rader == 1;
    }

    @Override
    public Sak hentFor(Saksnummer saksnummer) {
        var query = hentSakQuery(saksnummer);
        return hentEksaktResultat(query).map();
    }
}
