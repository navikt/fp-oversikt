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
        return entityManager.createQuery("from VedtakEntitet ", VedtakEntitet.class)
            .getResultList()
            .stream()
            .map(VedtakEntitet::map)
            .toList();
    }
}
