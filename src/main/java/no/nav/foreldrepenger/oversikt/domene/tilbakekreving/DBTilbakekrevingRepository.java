package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class DBTilbakekrevingRepository implements TilbakekrevingRepository {

    private EntityManager entityManager;

    @Inject
    public DBTilbakekrevingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    DBTilbakekrevingRepository() {
        //CDI
    }

    @Override
    public void lagre(Tilbakekreving tilbakekreving) {
        var query = hentTilbakekreving(tilbakekreving.saksnummer());
        var eksisterende = hentUniktResultat(query);
        if (eksisterende.isEmpty()) {
            entityManager.persist(new TilbakekrevingEntitet(tilbakekreving));
        } else {
            eksisterende.get().setJson(tilbakekreving);
            entityManager.merge(eksisterende.get());
        }
        entityManager.flush();
    }

    private TypedQuery<TilbakekrevingEntitet> hentTilbakekreving(Saksnummer saksnummer) {
        return entityManager.createQuery("from tilbakekreving where saksnummer =:saksnummer", TilbakekrevingEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
    }

    @Override
    public Set<Tilbakekreving> hentFor(Set<Saksnummer> saksnummer) {
        var query = entityManager.createQuery("from tilbakekreving where saksnummer in (:saksnummer)", TilbakekrevingEntitet.class);
        query.setParameter("saksnummer", saksnummer.stream().map(Saksnummer::value).toList());
        return query.getResultList()
            .stream()
            .map(TilbakekrevingEntitet::map)
            .collect(Collectors.toSet());
    }
}
