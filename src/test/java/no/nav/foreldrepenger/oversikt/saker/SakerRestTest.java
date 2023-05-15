package no.nav.foreldrepenger.oversikt.saker;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.oversikt.innhenting.BehandlingHendelseHåndterer.opprettTask;
import static no.nav.foreldrepenger.oversikt.innhenting.FpSak.BrukerRolle.MOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;
import no.nav.foreldrepenger.common.innsyn.Person;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.EsSak;
import no.nav.foreldrepenger.oversikt.innhenting.FpSak;
import no.nav.foreldrepenger.oversikt.innhenting.HentSakTask;
import no.nav.foreldrepenger.oversikt.innhenting.Sak;
import no.nav.foreldrepenger.oversikt.innhenting.SvpSak;
import no.nav.foreldrepenger.oversikt.innhenting.SøknadStatus;
import no.nav.foreldrepenger.oversikt.stub.FpsakTjenesteStub;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class SakerRestTest {

    @Test
    void hent_fp_sak_roundtrip_test() {
        var aktørId = AktørId.dummy();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, AktørId::value), () -> aktørId);


        var uttaksperiodeDto1 = new FpSak.Uttaksperiode(now().minusWeeks(4), now().minusWeeks(2), new FpSak.Uttaksperiode.Resultat(
            FpSak.Uttaksperiode.Resultat.Type.INNVILGET));
        var uttaksperioder = List.of(uttaksperiodeDto1);
        var vedtak = new FpSak.Vedtak(LocalDateTime.now(), uttaksperioder, FpSak.Vedtak.Dekningsgrad.HUNDRE);

        var aktørIdAnnenPart = AktørId.dummy();
        var aktørIdBarn = AktørId.dummy();
        var familieHendelse = new Sak.FamilieHendelse(now(), now().minusMonths(1), 1, null);
        var søknadsperiode = new FpSak.Søknad.Periode(now().minusMonths(1), now().plusMonths(1));
        var søknad = new FpSak.Søknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(søknadsperiode));
        var sakFraFpsak = new FpSak(Saksnummer.dummy().value(), aktørId.value(), familieHendelse, Sak.Status.AVSLUTTET, Set.of(vedtak), aktørIdAnnenPart.value(),
            ap(), Set.of(søknad), MOR, Set.of(aktørIdBarn.value()));
        sendBehandlingHendelse(sakFraFpsak, repository);

        var sakerFraDBtilDto = tjeneste.hent().foreldrepenger().stream().toList();

        assertThat(sakerFraDBtilDto).hasSize(1);
        var sakFraDbOmgjortTilDto = sakerFraDBtilDto.get(0);
        assertThat(sakFraDbOmgjortTilDto.saksnummer().value()).isEqualTo(sakFraFpsak.saksnummer());
        assertThat(sakFraDbOmgjortTilDto.sakAvsluttet()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder()).hasSameSizeAs(vedtak.uttaksperioder());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).fom()).isEqualTo(vedtak.uttaksperioder().get(0).fom());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).tom()).isEqualTo(vedtak.uttaksperioder().get(0).tom());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).resultat().innvilget()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).fom()).isBefore(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).tom());
        assertThat(sakFraDbOmgjortTilDto.annenPart().fnr().value()).isEqualTo(aktørIdAnnenPart.value());
        assertThat(sakFraDbOmgjortTilDto.barn()).containsExactly(new Person(new Fødselsnummer(aktørIdBarn.value()), null));
        assertThat(sakFraDbOmgjortTilDto.kanSøkeOmEndring()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());

        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.UNDER_BEHANDLING);

        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().søknadsperioder()).hasSize(1);
        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().søknadsperioder().get(0).fom()).isEqualTo(søknadsperiode.fom());
        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().søknadsperioder().get(0).tom()).isEqualTo(søknadsperiode.tom());

        assertThat(sakFraDbOmgjortTilDto.sakTilhørerMor()).isTrue();
    }

    private static Set<Sak.Aksjonspunkt> ap() {
        return Set.of(new Sak.Aksjonspunkt("5070", Sak.Aksjonspunkt.Status.OPPRETTET, null, LocalDateTime.now()));
    }

    @Test
    void hent_svp_sak_roundtrip_test() {
        var aktørId = AktørId.dummy();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, AktørId::value), () -> aktørId);

        var familieHendelse = new Sak.FamilieHendelse(now(), now().minusMonths(1), 1, null);
        var søknad = new SvpSak.Søknad(SøknadStatus.MOTTATT, LocalDateTime.now());
        var sakFraFpsak = new SvpSak(Saksnummer.dummy().value(), aktørId.value(), familieHendelse, Sak.Status.AVSLUTTET, ap(), Set.of(søknad));
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

        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.UNDER_BEHANDLING);
    }

    @Test
    void hent_es_sak_roundtrip_test() {
        var aktørId = AktørId.dummy();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, AktørId::value), () -> aktørId);

        var familieHendelse = new Sak.FamilieHendelse(now(), now().minusMonths(1), 1, null);
        var sakFraFpsak = new EsSak(Saksnummer.dummy().value(), aktørId.value(), familieHendelse, Sak.Status.AVSLUTTET, ap(),
            Set.of(new EsSak.Søknad(SøknadStatus.MOTTATT, LocalDateTime.now())));
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

        assertThat(sakFraDbOmgjortTilDto.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.UNDER_BEHANDLING);
    }

    private static void sendBehandlingHendelse(Sak fraFpsak, RepositoryStub sakRepository) {
        var saksnummer = new Saksnummer(fraFpsak.saksnummer());
        var prosessTaskData = opprettTask(UUID.randomUUID(), saksnummer);
        new HentSakTask(new FpsakTjenesteStub(Map.of(saksnummer, fraFpsak)), sakRepository).doTask(prosessTaskData);
    }

}
