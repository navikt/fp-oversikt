package no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.JpaExtension;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ExtendWith(JpaExtension.class)
class DBManglendeVedleggRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBManglendeVedleggRepository(entityManager);
        var saksnummer = Saksnummer.dummy();
        var før = List.of(DokumentType.I000042, DokumentType.I000036);
        repository.lagreManglendeVedleggPåSak(saksnummer, før);

        var manglendeVedlegg = repository.hentFor(saksnummer);
        assertThat(manglendeVedlegg).isEqualTo(før);
    }
}
