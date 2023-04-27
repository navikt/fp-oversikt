package no.nav.foreldrepenger.oversikt;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@ApplicationScoped
public class DBVedtakRepository implements VedtakRepository {

    private final EntityManager entityManager;

    @Inject
    public DBVedtakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lagre(Vedtak vedtak) {
        entityManager.persist(new VedtakEntitet(vedtak));
        entityManager.flush();
    }

    @Override
    public List<Vedtak> hentFor(String aktørId) {
        var query = entityManager.createNativeQuery("select * from vedtak where json->>'aktørId' = :aktørId", VedtakEntitet.class);
        query.setParameter("aktørId", aktørId);
        return ((List<VedtakEntitet>) query.getResultList())
            .stream()
            .map(VedtakEntitet::map)
            .toList();
    }
}
