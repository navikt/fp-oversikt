package no.nav.foreldrepenger.oversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JpaExtension.class)
class DBVedtakRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBVedtakRepository(entityManager);
        var aktørId = UUID.randomUUID().toString();
        var originalt = new VedtakV0("123", "status", "fp", aktørId);
        repository.lagre(originalt);
        repository.lagre(new VedtakV0("345", "status", "fp", UUID.randomUUID().toString()));

        var vedtakList = repository.hentFor(aktørId);

        assertThat(vedtakList).hasSize(1);
        assertThat(vedtakList.get(0)).isEqualTo(originalt);
    }
}
