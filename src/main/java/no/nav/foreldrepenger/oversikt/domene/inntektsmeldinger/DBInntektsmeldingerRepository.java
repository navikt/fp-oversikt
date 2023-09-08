package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class DBInntektsmeldingerRepository implements InntektsmeldingerRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DBInntektsmeldingerRepository.class);
    private EntityManager entityManager;

    @Inject
    public DBInntektsmeldingerRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    DBInntektsmeldingerRepository() {
        //CDI
    }

    @Override
    public void lagre(Saksnummer saksnummer, Set<Inntektsmelding> inntektsmeldinger) {
        var eksisterende = hentInntektsmeldinger(saksnummer);
        if (eksisterende.isEmpty()) {
            entityManager.persist(new InntektsmeldingerEntitet(saksnummer, inntektsmeldinger));
        } else {
            eksisterende.get().setJson(inntektsmeldinger);
            entityManager.merge(eksisterende.get());
        }
        entityManager.flush();
    }

    private Optional<InntektsmeldingerEntitet> hentInntektsmeldinger(Saksnummer saksnummer) {
        var query = entityManager.createQuery("from inntektsmeldinger where saksnummer =:saksnummer", InntektsmeldingerEntitet.class)
            .setParameter("saksnummer", saksnummer.value());
        return hentUniktResultat(query);
    }

    @Override
    public Set<Inntektsmelding> hentFor(Set<Saksnummer> saksnummer) {
        var entitet = hent(saksnummer);
        return entitet.map(InntektsmeldingerEntitet::map).orElse(Set.of());
    }

    @Override
    public void slett(Saksnummer saksnummer) {
        hent(Set.of(saksnummer)).ifPresent(ims -> {
            LOG.info("Sletter inntektsmeldinger for sak {}", saksnummer);
            entityManager.remove(ims);
            entityManager.flush();
        });
    }

    private Optional<InntektsmeldingerEntitet> hent(Set<Saksnummer> saksnummer) {
        var query = entityManager.createQuery("from inntektsmeldinger where saksnummer in (:saksnummer)", InntektsmeldingerEntitet.class);
        query.setParameter("saksnummer", saksnummer.stream().map(Saksnummer::value).toList());
        return hentUniktResultat(query);
    }
}
