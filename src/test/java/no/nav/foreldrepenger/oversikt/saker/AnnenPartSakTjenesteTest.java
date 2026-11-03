package no.nav.foreldrepenger.oversikt.saker;

import static java.time.LocalDateTime.now;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenpartUbeskyttetAdresse;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class AnnenPartSakTjenesteTest {

    @Test
    void henter_annen_parts_sak_på_barns_aktørId() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var aktørIdBarn = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var termindato = fødselsdato.minusWeeks(1);
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, aktørIdBarn, fødselsdato, now(), termindato);
        var annenPartsSakPåAnnetBarn = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato, now().minusYears(1),
            termindato.minusWeeks(1));
        repository.lagre(annenPartsSak);
        repository.lagre(annenPartsSakPåAnnetBarn);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, aktørIdBarn, null).orElseThrow();

        assertThat(annenPartSak.saksnummer()).isEqualTo(annenPartsSak.saksnummer());
    }

    @Test
    void henter_annen_parts_sak_på_familieHendelse() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var termindato = fødselsdato.minusWeeks(1);
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato, now(), termindato);
        var annenPartsSakPåAnnetBarn = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato.minusYears(1), now().minusYears(1),
            termindato.minusYears(1));
        repository.lagre(annenPartsSak);
        repository.lagre(annenPartsSakPåAnnetBarn);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, fødselsdato).orElseThrow();

        assertThat(annenPartSak.saksnummer()).isEqualTo(annenPartsSak.saksnummer());
    }

    @Test
    void henter_annen_parts_sak_på_omsorgsovertakelse_i_stedet_for_fødselsdato() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var omsorgsovertakelse = LocalDate.now().plusWeeks(1);
        var dekningsgrad = Dekningsgrad.HUNDRE;
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, now(), Set.of(), dekningsgrad, false);
        var annenPartsSak = new SakFP0(Saksnummer.dummy(), aktørIdAnnenPart, true,
            Set.of(new FpVedtak(now(), List.of(), dekningsgrad, null, null, null)), aktørIdSøker,
            new FamilieHendelse(fødselsdato, null, 1, omsorgsovertakelse), Set.of(), Set.of(søknad), BrukerRolle.MOR, Set.of(),
            new Rettigheter(false, false, false), false, now());
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, omsorgsovertakelse);

        assertThat(annenPartSak).map(sak -> sak.saksnummer()).contains(annenPartsSak.saksnummer());
    }

    @Test
    void henter_ikke_annen_parts_sak_hvis_oppgitt_annen_annen_part() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var tredjePart = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, tredjePart, null, fødselsdato, now(), fødselsdato);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, fødselsdato);

        assertThat(annenPartSak).isEmpty();
    }

    @Test
    void henter_ikke_annen_parts_sak_hvis_sak_ikke_inneholder_annen_part() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, null, fødselsdato, now(), fødselsdato);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, null, null, fødselsdato);

        assertThat(annenPartSak).isEmpty();
    }

    @Test
    void henter_ikke_annen_parts_sak_hvis_oppgitt_aleneomsorg() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato, now(), fødselsdato, true);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, fødselsdato);

        assertThat(annenPartSak).isEmpty();
    }

    @Test
    void finner_annen_parts_sak_uten_vedtak() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var termindato = LocalDate.of(2026, 5, 18); // mandag
        var antallBarn = 2;
        var søknadsperiode = new FpSøknadsperiode(termindato, LocalDate.of(2026, 7, 24), Konto.MØDREKVOTE, null, null, null, null, null, false, null);
        var annenPartsSak = sakUtenVedtak(aktørIdAnnenPart, aktørIdSøker, termindato, Dekningsgrad.ÅTTI, antallBarn, Set.of(søknadsperiode));
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, termindato).orElseThrow();

        assertThat(annenPartSak.saksnummer()).isEqualTo(annenPartsSak.saksnummer());
        assertThat(annenPartSak.gjeldendeVedtak()).isEmpty();
    }

    @Test
    void henter_annen_parts_sak_på_familieHendelse_hvis_sak_ikke_har_barns_aktørId() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var aktørIdBarn = AktørId.dummy();
        LocalDate termindato = fødselsdato.minusMonths(1);
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, null, fødselsdato, now(), termindato);
        var annenPartsSakPåAnnetBarn = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato.minusYears(1), now().minusYears(1),
            termindato);
        repository.lagre(annenPartsSak);
        repository.lagre(annenPartsSakPåAnnetBarn);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, aktørIdBarn, fødselsdato).orElseThrow();

        assertThat(annenPartSak.saksnummer()).isEqualTo(annenPartsSak.saksnummer());
    }

    @Test
    void henter_annen_parts_sak_på_nesten_lik_termindato() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var termindato = LocalDate.of(2025, 1, 1);
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), null, now(), termindato);
        var annenPartsSakPåAnnetBarn = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), termindato.minusYears(1), now().minusYears(1),
            termindato.minusYears(1));
        repository.lagre(annenPartsSak);
        repository.lagre(annenPartsSakPåAnnetBarn);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, termindato.plusWeeks(1));

        assertThat(annenPartSak).map(sak -> sak.saksnummer()).contains(annenPartsSak.saksnummer());
    }

    @Test
    void henter_annen_parts_sak_matcher_termindato_med_annen_parts_fødselsdato() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var termindato = LocalDate.of(2025, 1, 1);
        var termindatoAnnenPart = termindato.minusWeeks(4);
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), termindato.minusWeeks(1), now(), termindatoAnnenPart);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, termindato).orElseThrow();

        assertThat(annenPartSak.saksnummer()).isEqualTo(annenPartsSak.saksnummer());
    }

    @Test
    void henter_annen_parts_sak_på_nesten_lik_fødselsdato() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdatoAnnenpart = LocalDate.now();
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdatoAnnenpart, now(), null);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, fødselsdatoAnnenpart.plusWeeks(2));

        assertThat(annenPartSak).map(sak -> sak.saksnummer()).contains(annenPartsSak.saksnummer());
    }

    @Test
    void henter_ikke_annen_parts_sak_hvis_intervall_mellom_datoene_er_for_stort() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdatoAnnenpart = LocalDate.now();
        var annenPartsSak = sakMedVedtak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdatoAnnenpart, now(), null);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));
        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, null, fødselsdatoAnnenpart.plusWeeks(10));

        assertThat(annenPartSak).isEmpty();
    }

    @Test
    void velger_sak_med_nyeste_søknad_ved_flere_treff_på_samme_barn() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var aktørIdBarn = AktørId.dummy();
        var mottatt = LocalDateTime.of(2026, 9, 1, 12, 0);
        var eldre = sakForBarnMedSøknad(aktørIdAnnenPart, aktørIdSøker, aktørIdBarn, Set.of(søknad(mottatt)));
        var nyere = sakForBarnMedSøknad(aktørIdAnnenPart, aktørIdSøker, aktørIdBarn, Set.of(søknad(mottatt.plusDays(1))));
        var utenSøknad = sakForBarnMedSøknad(aktørIdAnnenPart, aktørIdSøker, aktørIdBarn, Set.of());
        repository.lagre(eldre);
        repository.lagre(nyere);
        repository.lagre(utenSøknad);
        var tjeneste = new AnnenPartSakTjeneste(new Saker(repository, myndigInnloggetBruker(), annenpartUbeskyttetAdresse()));

        var annenPartSak = tjeneste.annenPartGjeldendeSakOppgittSøker(aktørIdSøker, aktørIdAnnenPart, aktørIdBarn, null);

        assertThat(annenPartSak).map(SakFP0.class::cast).map(SakFP0::saksnummer).contains(nyere.saksnummer());
    }

    private static FpSøknad søknad(LocalDateTime mottatt) {
        return new FpSøknad(SøknadStatus.BEHANDLET, mottatt, Set.of(), Dekningsgrad.HUNDRE, false);
    }

    private static SakFP0 sakForBarnMedSøknad(AktørId aktørId, AktørId annenPartAktørId, AktørId aktørIdBarn, Set<FpSøknad> søknader) {
        return new SakFP0(Saksnummer.dummy(), aktørId, false, Set.of(), annenPartAktørId,
            new FamilieHendelse(null, LocalDate.of(2026, 10, 1), 1, null), Set.of(), søknader, BrukerRolle.MOR, Set.of(aktørIdBarn),
            new Rettigheter(false, false, false), false, now());
    }

    private static SakFP0 sakMedVedtak(AktørId aktørId,
                                       AktørId annenPartAktørId,
                                       AktørId aktørIdBarn,
                                       LocalDate fødselsdato,
                                       LocalDateTime vedtakstidspunkt,
                                       LocalDate termindato) {
        return sakMedVedtak(aktørId, annenPartAktørId, aktørIdBarn, fødselsdato, vedtakstidspunkt, termindato, false);
    }

    private static SakFP0 sakMedVedtak(AktørId aktørId,
                                       AktørId annenPartAktørId,
                                       AktørId aktørIdBarn,
                                       LocalDate fødselsdato,
                                       LocalDateTime vedtakstidspunkt,
                                       LocalDate termindato,
                                       boolean aleneomsorg) {
        var dekningsgrad = Dekningsgrad.HUNDRE;
        var vedtak = new FpVedtak(vedtakstidspunkt, List.of(), dekningsgrad, null, null, null);
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, now(), Set.of(), dekningsgrad, false);
        return new SakFP0(Saksnummer.dummy(), aktørId, true, Set.of(vedtak), annenPartAktørId, new FamilieHendelse(fødselsdato, termindato, 1, null),
            Set.of(), Set.of(søknad), BrukerRolle.MOR, aktørIdBarn == null ? Set.of() : Set.of(aktørIdBarn),
            new Rettigheter(aleneomsorg, false, false), false, now());
    }

    private static SakFP0 sakUtenVedtak(AktørId aktørId,
                                        AktørId annenPartAktørId,
                                        LocalDate termindato,
                                        Dekningsgrad dekningsgrad,
                                        int antallBarn,
                                        Set<FpSøknadsperiode> perioder) {
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, now(), perioder, dekningsgrad, false);
        return new SakFP0(Saksnummer.dummy(), aktørId, false, Set.of(), annenPartAktørId, new FamilieHendelse(null, termindato, antallBarn, null),
            Set.of(), Set.of(søknad), BrukerRolle.MOR, Set.of(), new Rettigheter(false, false, false), false, now());
    }

}
