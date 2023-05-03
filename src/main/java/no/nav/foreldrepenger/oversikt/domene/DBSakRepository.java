package no.nav.foreldrepenger.oversikt.domene;

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
        entityManager.persist(new SakEntitet(sak));
        entityManager.flush();
    }

    @Override
    public List<Sak> hentFor(AktørId aktørId) { // TODO: Endre fra vedtak til sak?
        var query = entityManager.createNativeQuery("select * from vedtak where json->>'aktørId' = :aktørId", SakEntitet.class);
        query.setParameter("aktørId", aktørId.value());
        return ((List<SakEntitet>) query.getResultList())
            .stream()
            .map(SakEntitet::map)
            .toList();
    }
}
