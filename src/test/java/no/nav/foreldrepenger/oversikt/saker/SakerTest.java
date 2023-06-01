package no.nav.foreldrepenger.oversikt.saker;

import static java.time.LocalDateTime.now;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.esSak;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.fh;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.fpSak;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.fpSakUtenSøknad;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.svpSak;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.EsSøknad;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.SakES0;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakSVP0;
import no.nav.foreldrepenger.oversikt.domene.SakStatus;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SvpSøknad;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class SakerTest {

    @Test
    void skal_ikke_returne_saker_uten_søknad() {
        var repository = new RepositoryStub();
        var aktørId = AktørId.dummy();
        repository.lagre(fpSak(aktørId));
        repository.lagre(fpSak(aktørId));
        repository.lagre(fpSakUtenSøknad(aktørId));
        repository.lagre(svpSak(aktørId));
        repository.lagre(esSak(aktørId));
        var saker = new Saker(repository, AktørId::value);

        var sakerDto = saker.hent(aktørId);

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).hasSize(1);
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Disabled
    @Test
    void skal_ikke_returnere_saker_henlagte_saker() {
        var aktørId = AktørId.dummy();
        var henlagtFpSak = new SakFP0(Saksnummer.dummy(), aktørId,
            SakStatus.AVSLUTTET, Set.of(), AktørId.dummy(), fh(), Set.of(), Set.of(new FpSøknad(SøknadStatus.BEHANDLET, now(),
            Set.of(), Dekningsgrad.HUNDRE)), BrukerRolle.MOR,
            Set.of(), new Rettigheter(false, false, false), false, now());
        var henlagtSvpSak = new SakSVP0(Saksnummer.dummy(), aktørId, SakStatus.AVSLUTTET, new FamilieHendelse(null, LocalDate.now(),
            0, null), Set.of(), Set.of(new SvpSøknad(SøknadStatus.BEHANDLET, now())), Set.of(), now());
        var henlagtEsSak = new SakES0(Saksnummer.dummy(), aktørId, SakStatus.AVSLUTTET, fh(), Set.of(), Set.of(new EsSøknad(SøknadStatus.BEHANDLET,
            now())), Set.of(), now());
        var repository = new RepositoryStub();
        repository.lagre(henlagtFpSak);
        repository.lagre(henlagtSvpSak);
        repository.lagre(henlagtEsSak);

        var saker = new Saker(repository, AktørId::value);
        var sakerDto = saker.hentSaker(aktørId);

        assertThat(sakerDto).isEmpty();
    }

}
