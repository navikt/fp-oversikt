package no.nav.foreldrepenger.oversikt.domene.beregning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

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
    public void lagre(Saksnummer saksnummer, Set<Beregning> beregninger) {
        var eksisterende = hentBeregninger(saksnummer);
        if (eksisterende.isEmpty()) {
            entityManager.persist(new BeregningerEntitet(saksnummer, beregninger));
        } else {
            eksisterende.get().setJson(beregninger);
            entityManager.merge(eksisterende.get());
        }
        entityManager.flush();
    }

    private Optional<BeregningerEntitet> hentBeregninger(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from beregninger where saksnummer =:saksnummer", BeregningerEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query);
    }

    @Override
    public Set<Beregning> hentFor(Set<Saksnummer> saksnummer) {
        var entitet = hent(saksnummer);
        return entitet.map(BeregningerEntitet::map).orElse(Set.of());
    }

    @Override
    public void slett(Saksnummer saksnummer) {
        hent(Set.of(saksnummer)).ifPresent(ims -> {
            LOG.info("Sletter beregninger for sak {}", saksnummer);
            entityManager.remove(ims);
            entityManager.flush();
        });
    }

    private Optional<BeregningerEntitet> hent(Set<Saksnummer> saksnummer) {
        var query = entityManager.createQuery("from beregninger where saksnummer in (:saksnummer)", BeregningerEntitet.class);
        query.setParameter("saksnummer", saksnummer.stream().map(Saksnummer::value).toList());
        return hentUniktResultat(query);
    }


}
