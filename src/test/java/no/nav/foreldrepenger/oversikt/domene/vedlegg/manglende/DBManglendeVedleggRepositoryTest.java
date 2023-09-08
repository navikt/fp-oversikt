package no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.JpaExtension;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
