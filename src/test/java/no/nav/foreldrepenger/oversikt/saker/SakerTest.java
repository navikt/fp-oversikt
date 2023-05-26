package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.esSak;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.fpSak;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.fpSakUtenSøknad;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.svpSak;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class SakerTest {

    private static final AktørId AKTØR_ID_DUMMY = AktørId.dummy();

    @Test
    void skal_ikke_returne_saker_uten_søknad() {
        var repository = new RepositoryStub();
        repository.lagre(fpSak(AKTØR_ID_DUMMY));
        repository.lagre(fpSak(AKTØR_ID_DUMMY));
        repository.lagre(fpSakUtenSøknad(AKTØR_ID_DUMMY));
        repository.lagre(svpSak(AKTØR_ID_DUMMY));
        repository.lagre(esSak(AKTØR_ID_DUMMY));
        var saker = new Saker(repository, AktørId::value);

        var sakerDto = saker.hent(AKTØR_ID_DUMMY);

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).hasSize(1);
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

}
