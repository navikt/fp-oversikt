package no.nav.foreldrepenger.oversikt.soknadsplan;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class DBSøknadsplanRepository implements SøknadsplanRepository {

    private EntityManager entityManager;

    DBSøknadsplanRepository() {
        // CDI
    }

    @Inject
    public DBSøknadsplanRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lagre(Saksnummer saksnummer, Søknadsplan søknadsplan) {
        hentAktivEntitet(saksnummer).ifPresent(aktiv -> {
            aktiv.deaktiver();
            // Deaktiveringen må skrives før ny plan, ellers bryter insert den unike indeksen på aktiv plan per sak
            entityManager.flush();
        });
        entityManager.persist(new SøknadsplanEntitet(saksnummer, søknadsplan));
        entityManager.flush();
    }

    @Override
    public Optional<Søknadsplan> hentAktivFor(Saksnummer saksnummer) {
        return hentAktivEntitet(saksnummer).map(SøknadsplanEntitet::map);
    }

    private Optional<SøknadsplanEntitet> hentAktivEntitet(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from soknadsplan where saksnummer = :saksnummer and aktiv = true", SøknadsplanEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query);
    }
}
