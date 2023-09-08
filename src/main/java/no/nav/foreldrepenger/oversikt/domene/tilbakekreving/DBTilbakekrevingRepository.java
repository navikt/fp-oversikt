package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class DBTilbakekrevingRepository implements TilbakekrevingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DBTilbakekrevingRepository.class);

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
        var eksisterende = hentTilbakekreving(tilbakekreving.saksnummer());
        if (eksisterende.isEmpty()) {
            entityManager.persist(new TilbakekrevingEntitet(tilbakekreving));
        } else {
            eksisterende.get().setJson(tilbakekreving);
            entityManager.merge(eksisterende.get());
        }
        entityManager.flush();
    }

    private Optional<TilbakekrevingEntitet> hentTilbakekreving(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from tilbakekreving where saksnummer =:saksnummer", TilbakekrevingEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query);
    }

    @Override
    public Set<Tilbakekreving> hentFor(Set<Saksnummer> saksnummer) {
        var query = entityManager.createQuery("from tilbakekreving where saksnummer in (:saksnummer)", TilbakekrevingEntitet.class);
        query.setParameter("saksnummer", saksnummer.stream().map(Saksnummer::value).toList());
        return query.getResultStream()
            .map(TilbakekrevingEntitet::map)
            .collect(Collectors.toSet());
    }

    @Override
    public void slett(Saksnummer saksnummer) {
        hentTilbakekreving(saksnummer).ifPresent(tbk -> {
            LOG.info("Sletter tilbakekreving for sak {}", saksnummer);
            entityManager.remove(tbk);
            entityManager.flush();
        });
    }
}
