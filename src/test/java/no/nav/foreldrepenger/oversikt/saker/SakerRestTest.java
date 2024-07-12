package no.nav.foreldrepenger.oversikt.saker;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.oversikt.innhenting.BehandlingHendelseHåndterer.opprettHentSakTask;
import static no.nav.foreldrepenger.oversikt.innhenting.FpSak.BrukerRolle.MOR;
import static no.nav.foreldrepenger.oversikt.innhenting.FpSak.Uttaksperiode.Resultat.Type;
import static no.nav.foreldrepenger.oversikt.innhenting.FpSak.Uttaksperiode.Resultat.Årsak;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;
import no.nav.foreldrepenger.common.innsyn.Dekningsgrad;
import no.nav.foreldrepenger.common.innsyn.KontoType;
import no.nav.foreldrepenger.common.innsyn.Person;
import no.nav.foreldrepenger.common.innsyn.RettighetType;
import no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode;
import no.nav.foreldrepenger.common.innsyn.svp.Vedtak;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.fp.Trekkdager;
import no.nav.foreldrepenger.oversikt.innhenting.EsSak;
import no.nav.foreldrepenger.oversikt.innhenting.FpSak;
import no.nav.foreldrepenger.oversikt.innhenting.FpSak.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.innhenting.FpSak.Uttaksperiode.Resultat;
import no.nav.foreldrepenger.oversikt.innhenting.HentSakTask;
import no.nav.foreldrepenger.oversikt.innhenting.Konto;
import no.nav.foreldrepenger.oversikt.innhenting.MorsAktivitet;
import no.nav.foreldrepenger.oversikt.innhenting.OppholdÅrsak;
import no.nav.foreldrepenger.oversikt.innhenting.OverføringÅrsak;
import no.nav.foreldrepenger.oversikt.innhenting.Sak;
import no.nav.foreldrepenger.oversikt.innhenting.SvpSak;
import no.nav.foreldrepenger.oversikt.innhenting.SøknadStatus;
import no.nav.foreldrepenger.oversikt.innhenting.UtsettelseÅrsak;
import no.nav.foreldrepenger.oversikt.stub.FpsakTjenesteStub;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;
import no.nav.foreldrepenger.oversikt.stub.TilgangKontrollStub;

class SakerRestTest {

    @Test
    void hent_fp_sak_roundtrip_test() {
        var innloggetBruker = myndigInnloggetBruker();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, innloggetBruker, TilgangKontrollStub.borger(true)), TilgangKontrollStub.borger(true));

        var arbeidstidsprosent = new Prosent(BigDecimal.valueOf(33.33));
        var aktivitet = new FpSak.UttakAktivitet(FpSak.UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), UUID.randomUUID().toString());
        var uttaksperiodeDto = new Uttaksperiode(now().minusWeeks(4), now().minusWeeks(2), UtsettelseÅrsak.FRI,
            OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER, OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER, null, true, MorsAktivitet.INNLAGT,
            new Resultat(Type.INNVILGET_GRADERING, Årsak.ANNET,
                Set.of(new Uttaksperiode.UttaksperiodeAktivitet(aktivitet, Konto.FORELDREPENGER, new Trekkdager(10), arbeidstidsprosent)), false));
        var uttaksperioder = List.of(uttaksperiodeDto);
        var vedtak = new FpSak.Vedtak(LocalDateTime.now(), uttaksperioder, FpSak.Dekningsgrad.HUNDRE);

        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdBarn = AktørId.dummy();
        var familieHendelse = new Sak.FamilieHendelse(now(), now().minusMonths(1), 1, null);
        var søknadsperiode = new FpSak.Søknad.Periode(now().minusMonths(1), now().plusMonths(1), Konto.FORELDREPENGER, UtsettelseÅrsak.SØKER_SYKDOM,
            OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, OverføringÅrsak.SYKDOM_ANNEN_FORELDER, new FpSak.Gradering(arbeidstidsprosent, new FpSak.UttakAktivitet(
            FpSak.UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null)), new Prosent(40), true, MorsAktivitet.ARBEID);
        var søknad = new FpSak.Søknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(søknadsperiode), FpSak.Dekningsgrad.ÅTTI);
        var sakFraFpsak = new FpSak(Saksnummer.dummy().value(), innloggetBruker.aktørId().value(), familieHendelse, true, Set.of(vedtak), aktørIdAnnenPart.value(),
            ventTidligSøknadAp(), Set.of(søknad), MOR, Set.of(aktørIdBarn.value()), new FpSak.Rettigheter(false, true, true), true);
        sendBehandlingHendelse(sakFraFpsak, repository);

        var sakerFraDBtilDto = tjeneste.hent().foreldrepenger().stream().toList();

        assertThat(sakerFraDBtilDto).hasSize(1);
        var sakFraDbOmgjortTilDto = sakerFraDBtilDto.get(0);
        assertThat(sakFraDbOmgjortTilDto.saksnummer().value()).isEqualTo(sakFraFpsak.saksnummer());
        assertThat(sakFraDbOmgjortTilDto.sakAvsluttet()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.dekningsgrad()).isEqualTo(Dekningsgrad.HUNDRE);
        var vedtaksperioder = sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder();
        assertThat(vedtaksperioder).hasSameSizeAs(vedtak.uttaksperioder());
        assertThat(vedtaksperioder.get(0).fom()).isEqualTo(vedtak.uttaksperioder().get(0).fom());
        assertThat(vedtaksperioder.get(0).tom()).isEqualTo(vedtak.uttaksperioder().get(0).tom());
        assertThat(vedtaksperioder.get(0).resultat().innvilget()).isTrue();
        assertThat(vedtaksperioder.get(0).resultat().trekkerDager()).isTrue();
        assertThat(vedtaksperioder.get(0).kontoType()).isEqualTo(KontoType.FORELDREPENGER);
        assertThat(vedtaksperioder.get(0).gradering().arbeidstidprosent().value()).isEqualTo(arbeidstidsprosent.decimalValue());
        assertThat(vedtaksperioder.get(0).utsettelseÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.FRI);
        assertThat(vedtaksperioder.get(0).oppholdÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER);
        assertThat(vedtaksperioder.get(0).overføringÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER);
        assertThat(vedtaksperioder.get(0).samtidigUttak()).isNull();
        assertThat(vedtaksperioder.get(0).morsAktivitet()).isEqualTo(no.nav.foreldrepenger.common.innsyn.MorsAktivitet.INNLAGT);
        assertThat(sakFraDbOmgjortTilDto.annenPart().fnr().value()).isEqualTo(aktørIdAnnenPart.value());
        assertThat(sakFraDbOmgjortTilDto.barn()).containsExactly(new Person(new Fødselsnummer(aktørIdBarn.value()), null));
        assertThat(sakFraDbOmgjortTilDto.kanSøkeOmEndring()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());

        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.VENT_TIDLIG_SØKNAD);

        assertThat(sakFraDbOmgjortTilDto.sakTilhørerMor()).isTrue();

        assertThat(sakFraDbOmgjortTilDto.rettighetType()).isEqualTo(RettighetType.BARE_SØKER_RETT);
        assertThat(sakFraDbOmgjortTilDto.morUføretrygd()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.harAnnenForelderTilsvarendeRettEØS()).isTrue();

        assertThat(sakFraDbOmgjortTilDto.ønskerJustertUttakVedFødsel()).isTrue();

    }

    private static Set<Sak.Aksjonspunkt> ventTidligSøknadAp() {
        return Set.of(new Sak.Aksjonspunkt(Sak.Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD, null, LocalDateTime.now()));
    }

    @Test
    void hent_svp_sak_roundtrip_test() {
        var innloggetBruker = myndigInnloggetBruker();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, innloggetBruker, TilgangKontrollStub.borger(true)), TilgangKontrollStub.borger(true));

        var familieHendelse = new Sak.FamilieHendelse(now(), now().minusMonths(1), 1, null);
        var aktivitet = new SvpSak.Aktivitet(SvpSak.Aktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null);
        var oppholdsperioder = Set.of(new SvpSak.OppholdPeriode(now(), now(), SvpSak.OppholdPeriode.Årsak.FERIE, SvpSak.OppholdPeriode.OppholdKilde.SAKSBEHANDLER));
        var tilrettelegging = new SvpSak.Søknad.Tilrettelegging(aktivitet, now(), "risiko", "tiltak",
            Set.of(new SvpSak.Søknad.Tilrettelegging.Periode(now(), SvpSak.TilretteleggingType.DELVIS, new Prosent(50))), oppholdsperioder);
        var søknad = new SvpSak.Søknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(tilrettelegging));
        var arbeidsforholdUttak = new SvpSak.Vedtak.ArbeidsforholdUttak(aktivitet, now(), "risk", "til", Set.of(
            new SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode(now(), now(), SvpSak.TilretteleggingType.DELVIS, new Prosent(50), new Prosent(50),
                SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.INNVILGET)), oppholdsperioder, null);
        var sakFraFpsak = new SvpSak(Saksnummer.dummy().value(), innloggetBruker.aktørId().value(), familieHendelse,
            true, ventTidligSøknadAp(), Set.of(søknad), Set.of(new SvpSak.Vedtak(LocalDateTime.now(), Set.of(arbeidsforholdUttak), SvpSak.Vedtak.AvslagÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE)));
        sendBehandlingHendelse(sakFraFpsak, repository);

        var sakerFraDBtilDto = tjeneste.hent().svangerskapspenger().stream().toList();

        assertThat(sakerFraDBtilDto).hasSize(1);
        var sakFraDbOmgjortTilDto = sakerFraDBtilDto.get(0);
        assertThat(sakFraDbOmgjortTilDto.saksnummer().value()).isEqualTo(sakFraFpsak.saksnummer());
        assertThat(sakFraDbOmgjortTilDto.sakAvsluttet()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().arbeidsforhold().stream().toList().get(0).oppholdsperioder().stream().toList().get(0).oppholdKilde()).isEqualTo(
            OppholdPeriode.OppholdKilde.SAKSBEHANDLER);

        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.VENT_TIDLIG_SØKNAD);

        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().avslagÅrsak()).isEqualTo(Vedtak.AvslagÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE);
    }

    @Test
    void hent_es_sak_roundtrip_test() {
        var innloggetBruker = myndigInnloggetBruker();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, innloggetBruker, TilgangKontrollStub.borger(true)), TilgangKontrollStub.borger(true));

        var familieHendelse = new Sak.FamilieHendelse(now(), now().minusMonths(1), 1, null);
        var sakFraFpsak = new EsSak(Saksnummer.dummy().value(), innloggetBruker.aktørId().value(), familieHendelse, true, ventTidligSøknadAp(),
            Set.of(new EsSak.Søknad(SøknadStatus.MOTTATT, LocalDateTime.now())), Set.of(new EsSak.Vedtak(LocalDateTime.now())));
        sendBehandlingHendelse(sakFraFpsak, repository);

        var sakerFraDBtilDto = tjeneste.hent().engangsstønad().stream().toList();

        assertThat(sakerFraDBtilDto).hasSize(1);
        var sakFraDbOmgjortTilDto = sakerFraDBtilDto.get(0);
        assertThat(sakFraDbOmgjortTilDto.saksnummer().value()).isEqualTo(sakFraFpsak.saksnummer());
        assertThat(sakFraDbOmgjortTilDto.sakAvsluttet()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());

        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.VENT_TIDLIG_SØKNAD);
    }

    private static void sendBehandlingHendelse(Sak fraFpsak, RepositoryStub sakRepository) {
        var saksnummer = new Saksnummer(fraFpsak.saksnummer());
        var prosessTaskData = opprettHentSakTask(UUID.randomUUID(), saksnummer);
        var fpsakTjeneste = new FpsakTjenesteStub()
            .leggTilSak(fraFpsak);
        new HentSakTask(fpsakTjeneste, sakRepository).doTask(prosessTaskData);
    }

}
