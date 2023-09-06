package no.nav.foreldrepenger.oversikt.domene;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.foreldrepenger.common.domain.felles.DokumentType;

import java.util.List;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

@ApplicationScoped
public class DBSakRepository implements SakRepository {

    private final EntityManager entityManager;

    @Inject
    public DBSakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lagre(Sak sak) {
        var query = hentSak(sak.saksnummer());
        var eksisterendeSak = hentUniktResultat(query);
        if (eksisterendeSak.isEmpty()) {
            entityManager.persist(new SakEntitet(sak));
        } else {
            eksisterendeSak.get().setJson(sak);
            entityManager.merge(eksisterendeSak.get());
        }
        entityManager.flush();
    }

    private TypedQuery<SakEntitet> hentSak(Saksnummer saksnummer) {
        return entityManager.createQuery("from sak where saksnummer =:saksnummer", SakEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
    }

    @Override
    public void lagreManglendeVedleggPåSak(String saksnummer, List<DokumentType> manglendeVedlegg) {
        var query = entityManager.createNativeQuery("from sak where saksnummer =:saksnummer").setParameter("saksnummer", saksnummer);
        // TODO
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
