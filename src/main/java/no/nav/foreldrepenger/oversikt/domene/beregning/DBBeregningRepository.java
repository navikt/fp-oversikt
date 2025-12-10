package no.nav.foreldrepenger.oversikt.domene.beregning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

@ApplicationScoped
public class DBBeregningRepository implements BeregningRepository {


    private static final Logger LOG = LoggerFactory.getLogger(
        DBBeregningRepository.class);
    private EntityManager entityManager;

    @Inject
    public DBBeregningRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    DBBeregningRepository() {
        //CDI
    }

    @Override
    public void lagre(Saksnummer saksnummer, Beregning beregning) {
        var eksisterende = hentBeregning(saksnummer);
        if (eksisterende.isEmpty()) {
            entityManager.persist(new BeregningEntitet(saksnummer, beregning));
        } else {
            eksisterende.get().setJson(beregning);
            entityManager.merge(eksisterende.get());
        }
        entityManager.flush();
    }

    private Optional<BeregningEntitet> hentBeregning(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from beregning where saksnummer =:saksnummer", BeregningEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query);
    }

    @Override
    public Optional<Beregning> hentFor(Saksnummer saksnummer) {
        return hent(saksnummer).map(BeregningEntitet::map);
    }

    @Override
    public void slett(Saksnummer saksnummer) {
        hent(saksnummer).ifPresent(ims -> {
            LOG.info("Sletter beregning for sak {}", saksnummer);
            entityManager.remove(ims);
            entityManager.flush();
        });
    }

    private Optional<BeregningEntitet> hent(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from beregning where saksnummer =:saksnummer", BeregningEntitet.class);
        query.setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query);
    }


}
