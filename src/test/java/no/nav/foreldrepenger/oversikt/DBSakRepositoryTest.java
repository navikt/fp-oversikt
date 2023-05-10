package no.nav.foreldrepenger.oversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.DBSakRepository;
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.Vedtak;

@ExtendWith(JpaExtension.class)
class DBSakRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var aktørId = AktørId.dummy();
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(
            Uttaksperiode.Resultat.Type.INNVILGET)));
        var vedtak = new Vedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var originalt = new SakFP0(Saksnummer.dummy(), aktørId, Set.of(vedtak), AktørId.dummy());
        repository.lagre(originalt);
        var annenAktørsSak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), null, AktørId.dummy());
        repository.lagre(annenAktørsSak);

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.get(0)).isEqualTo(originalt);
    }

    @Test
    void oppdatererJsonPåSak(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var aktørId = AktørId.dummy();
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(
            Uttaksperiode.Resultat.Type.AVSLÅTT)));
        var vedtak = new Vedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var saksnummer = Saksnummer.dummy();
        var annenPartAktørId = AktørId.dummy();
        var originalt = new SakFP0(saksnummer, aktørId, Set.of(vedtak), annenPartAktørId);
        repository.lagre(originalt);
        var oppdatertSak = new SakFP0(saksnummer, aktørId, null, annenPartAktørId);
        repository.lagre(oppdatertSak);

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.get(0)).isNotEqualTo(originalt);
        assertThat(saker.get(0)).isEqualTo(oppdatertSak);
    }
}
