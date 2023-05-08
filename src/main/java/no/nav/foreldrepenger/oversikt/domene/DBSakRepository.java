package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@ApplicationScoped
public class DBSakRepository implements SakRepository {

    private final EntityManager entityManager;

    @Inject
    public DBSakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lagre(Sak sak) {
        var query = entityManager.createQuery("from sak where saksnummer =:saksnummer", SakEntitet.class).setParameter("saksnummer", sak.saksnummer().value());
        var eksisterendeSak = hentUniktResultat(query);
        if (eksisterendeSak.isEmpty()) {
            entityManager.persist(new SakEntitet(sak));
        } else {
            eksisterendeSak.get().setJson(sak);
            entityManager.merge(eksisterendeSak.get());
        }
        entityManager.flush();
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
}
