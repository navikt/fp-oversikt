package no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;

@ApplicationScoped
public class DBManglendeVedleggRepository implements ManglendeVedleggRepository {

    private final EntityManager entityManager;

    @Inject
    public DBManglendeVedleggRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public void lagreManglendeVedleggPåSak(Saksnummer saksnummer, List<DokumentType> manglendeVedlegg) {
        var query = hentManglendeVedlegg(saksnummer);
        var eksisterendeManglendeVedlegg = hentUniktResultat(query);
        if (eksisterendeManglendeVedlegg.isEmpty()) {
            entityManager.persist(new ManglendeVedleggEntitet(saksnummer.value(), manglendeVedlegg));
        } else {
            eksisterendeManglendeVedlegg.get().setJson(manglendeVedlegg);
            entityManager.merge(eksisterendeManglendeVedlegg.get());
        }
        entityManager.flush();
    }

    private TypedQuery<ManglendeVedleggEntitet> hentManglendeVedlegg(Saksnummer saksnummer) {
        return entityManager.createQuery("from manglendeVedlegg where saksnummer =:saksnummer", ManglendeVedleggEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
    }

    @Override
    public List<DokumentType> hentFor(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from manglendeVedlegg where saksnummer in (:saksnummer)", ManglendeVedleggEntitet.class);
        query.setParameter("saksnummer", saksnummer.value());
        return hentEksaktResultat(query).manglendeVedlegg();
    }
}
