package no.nav.foreldrepenger.oversikt.saker;

import static java.time.LocalDateTime.now;
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
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class AnnenPartVedtakTjenesteTest {

    @Test
    void henter_annen_parts_vedtak_på_barns_aktørId() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var aktørIdBarn = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var termindato = fødselsdato.minusWeeks(1);
        var annenPartsSak = sak(aktørIdAnnenPart, aktørIdSøker, aktørIdBarn, fødselsdato, now(), termindato);
        var annenPartsSakPåAnnetBarn = sak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato, now().minusYears(1),
            termindato.minusWeeks(1));
        repository.lagre(annenPartsSak);
        repository.lagre(annenPartsSakPåAnnetBarn);
        var tjeneste = new AnnenPartVedtakTjeneste(new Saker(repository, AktørId::value));
        var vedtak = tjeneste.hentFor(aktørIdSøker, aktørIdAnnenPart, aktørIdBarn, null);

        assertThat(vedtak.orElseThrow().termindato()).isEqualTo(termindato);
    }

    @Test
    void henter_annen_parts_vedtak_på_familieHendelse() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var termindato = fødselsdato.minusWeeks(1);
        var annenPartsSak = sak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato, now(), termindato);
        var annenPartsSakPåAnnetBarn = sak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato.minusYears(1), now().minusYears(1),
            termindato.minusYears(1));
        repository.lagre(annenPartsSak);
        repository.lagre(annenPartsSakPåAnnetBarn);
        var tjeneste = new AnnenPartVedtakTjeneste(new Saker(repository, AktørId::value));
        var vedtak = tjeneste.hentFor(aktørIdSøker, aktørIdAnnenPart, null, fødselsdato);

        assertThat(vedtak.orElseThrow().termindato()).isEqualTo(termindato);
    }

    @Test
    void henter_ikke_annen_parts_hvis_oppgitt_annen_annen_part() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var tredjePart = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var annenPartsSak = sak(aktørIdAnnenPart, tredjePart, null, fødselsdato, now(), fødselsdato);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartVedtakTjeneste(new Saker(repository, AktørId::value));
        var vedtak = tjeneste.hentFor(aktørIdSøker, aktørIdAnnenPart, null, fødselsdato);

        assertThat(vedtak).isEmpty();
    }

    @Test
    void henter_ikke_annen_parts_hvis_sak_ikke_inneholder_annen_part() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var annenPartsSak = sak(aktørIdAnnenPart, aktørIdSøker, null, fødselsdato, now(), fødselsdato);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartVedtakTjeneste(new Saker(repository, AktørId::value));
        var vedtak = tjeneste.hentFor(aktørIdSøker, null, null, fødselsdato);

        assertThat(vedtak).isEmpty();
    }

    @Test
    void henter_ikke_annen_parts_vedtak_hvis_oppgitt_aleneomsorg() {
        var repository = new RepositoryStub();
        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdSøker = AktørId.dummy();
        var fødselsdato = LocalDate.now();
        var annenPartsSak = sak(aktørIdAnnenPart, aktørIdSøker, AktørId.dummy(), fødselsdato, now(), fødselsdato, true);
        repository.lagre(annenPartsSak);
        var tjeneste = new AnnenPartVedtakTjeneste(new Saker(repository, AktørId::value));
        var vedtak = tjeneste.hentFor(aktørIdSøker, aktørIdAnnenPart, null, fødselsdato);

        assertThat(vedtak).isEmpty();
    }

    private static SakFP0 sak(AktørId aktørId,
                              AktørId annenPartAktørId,
                              AktørId aktørIdBarn,
                              LocalDate fødselsdato,
                              LocalDateTime vedtakstidspunkt,
                              LocalDate termindato) {
        return sak(aktørId, annenPartAktørId, aktørIdBarn, fødselsdato, vedtakstidspunkt, termindato, false);
    }

    private static SakFP0 sak(AktørId aktørId,
                              AktørId annenPartAktørId,
                              AktørId aktørIdBarn,
                              LocalDate fødselsdato,
                              LocalDateTime vedtakstidspunkt,
                              LocalDate termindato,
                              boolean aleneomsorg) {
        var dekningsgrad = Dekningsgrad.HUNDRE;
        var vedtak = new FpVedtak(vedtakstidspunkt, List.of(), dekningsgrad);
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, now(), Set.of(), dekningsgrad);
        return new SakFP0(Saksnummer.dummy(), aktørId, true, Set.of(vedtak), annenPartAktørId,
            new FamilieHendelse(fødselsdato, termindato, 1, null), Set.of(), Set.of(søknad), BrukerRolle.MOR,
            aktørIdBarn == null ? Set.of() : Set.of(aktørIdBarn), new Rettigheter(aleneomsorg, false, false), false, now());
    }

}
