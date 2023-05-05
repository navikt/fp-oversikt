package no.nav.foreldrepenger.oversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.DBSakRepository;
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.Uttak;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.Vedtak;

@ExtendWith(JpaExtension.class)
class DBSakRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var aktørId = new AktørId(UUID.randomUUID().toString());
        var uttak = new Uttak(Dekningsgrad.HUNDRE, List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2))));
        var vedtak = new Vedtak(LocalDateTime.now(), uttak);
        var originalt = new SakFP0(new Saksnummer("123"), aktørId, Set.of(vedtak));
        repository.lagre(originalt);
        repository.lagre(new SakFP0(new Saksnummer("345"), new AktørId(UUID.randomUUID().toString()), null));

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.get(0)).isEqualTo(originalt);
    }
}
