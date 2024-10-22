package no.nav.foreldrepenger.oversikt.saker;

import static java.time.LocalDateTime.now;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.esSak;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.fh;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.fpSak;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.fpSakUtenSøknad;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapperTest.svpSak;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.es.EsSøknad;
import no.nav.foreldrepenger.oversikt.domene.es.SakES0;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.svp.Aktivitet;
import no.nav.foreldrepenger.oversikt.domene.svp.SakSVP0;
import no.nav.foreldrepenger.oversikt.domene.svp.SvpSøknad;
import no.nav.foreldrepenger.oversikt.domene.svp.Tilrettelegging;
import no.nav.foreldrepenger.oversikt.domene.svp.TilretteleggingPeriode;
import no.nav.foreldrepenger.oversikt.domene.svp.TilretteleggingType;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;
import no.nav.foreldrepenger.oversikt.stub.TilgangKontrollStub;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontroll;

class SakerTest {

    private static final TilgangKontroll TILGANG_KONTROLL_DUMMY = TilgangKontrollStub.borger(true);

    @Test
    void skal_ikke_returne_saker_uten_søknad() {
        var repository = new RepositoryStub();
        var aktørId = AktørId.dummy();
        repository.lagre(fpSak(aktørId));
        repository.lagre(fpSak(aktørId));
        repository.lagre(fpSakUtenSøknad(aktørId));
        repository.lagre(svpSak(aktørId));
        repository.lagre(esSak(aktørId));
        var innloggetBruker = myndigInnloggetBruker(aktørId);
        var saker = new Saker(repository, innloggetBruker, TILGANG_KONTROLL_DUMMY);

        var sakerDto = saker.hent();

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).hasSize(1);
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_returnere_saker_henlagte_saker() {
        var aktørId = AktørId.dummy();
        var henlagtFpSak = new SakFP0(Saksnummer.dummy(), aktørId,
            true, Set.of(), AktørId.dummy(), fh(), Set.of(), Set.of(new FpSøknad(SøknadStatus.BEHANDLET, now(),
            Set.of(), Dekningsgrad.HUNDRE)), BrukerRolle.MOR,
            Set.of(), new Rettigheter(false, false, false), false, now());
        var henlagtSvpSak = new SakSVP0(Saksnummer.dummy(), aktørId, true, new FamilieHendelse(null, LocalDate.now(),
            0, null), Set.of(), Set.of(new SvpSøknad(SøknadStatus.BEHANDLET, now(), Set.of())), Set.of(), now());
        var henlagtEsSak = new SakES0(Saksnummer.dummy(), aktørId, true, fh(), Set.of(), Set.of(new EsSøknad(SøknadStatus.BEHANDLET,
            now())), Set.of(), now());
        var repository = new RepositoryStub();
        repository.lagre(henlagtFpSak);
        repository.lagre(henlagtSvpSak);
        repository.lagre(henlagtEsSak);

        var saker = new Saker(repository, myndigInnloggetBruker(), TILGANG_KONTROLL_DUMMY);
        var sakerDto = saker.hentSaker(aktørId);

        assertThat(sakerDto).isEmpty();
    }

    @Test
    void skal_ikke_returne_saker_med_søknad_men_uten_familiehendelse() {
        //FAGSYSTEM-315288 upunchet papirsøknad på henlagt digital søknad gir problemer. Søknad finnes, men ikke punchet familiehendelse i aktiv behandling
        var repository = new RepositoryStub();
        var aktørId = AktørId.dummy();
        var søknadFP = new FpSøknad(SøknadStatus.BEHANDLET, now(),
            Set.of(new FpSøknadsperiode(LocalDate.now(), LocalDate.now(), Konto.MØDREKVOTE, null, null, null, null, null, false, null)),
            Dekningsgrad.ÅTTI);
        repository.lagre(new SakFP0(Saksnummer.dummy(), aktørId, false, Set.of(), null, null, Set.of(), Set.of(søknadFP),
            BrukerRolle.MOR, Set.of(), null, false, now()));
        var svpSøknad = new SvpSøknad(SøknadStatus.MOTTATT, now(), Set.of(
            new Tilrettelegging(new Aktivitet(Aktivitet.Type.FRILANS, null, null), LocalDate.now(), null, null,
                Set.of(new TilretteleggingPeriode(LocalDate.now(), TilretteleggingType.INGEN, Prosent.ZERO)), Set.of())));
        repository.lagre(new SakSVP0(Saksnummer.dummy(), aktørId, false, null, Set.of(), Set.of(svpSøknad), Set.of(), now()));
        repository.lagre(new SakES0(Saksnummer.dummy(), aktørId, false, null, Set.of(), Set.of(new EsSøknad(SøknadStatus.MOTTATT, now())),
            Set.of(), now()));
        var innloggetBruker = myndigInnloggetBruker(aktørId);
        var saker = new Saker(repository, innloggetBruker, TILGANG_KONTROLL_DUMMY);

        var sakerDto = saker.hent();

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

}
