package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.innhenting.BehandlingHendelseHåndterer.opprettTask;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.EsSak;
import no.nav.foreldrepenger.oversikt.innhenting.FpSak;
import no.nav.foreldrepenger.oversikt.innhenting.HentSakTask;
import no.nav.foreldrepenger.oversikt.innhenting.Sak;
import no.nav.foreldrepenger.oversikt.innhenting.SvpSak;
import no.nav.foreldrepenger.oversikt.stub.FpsakTjenesteStub;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class SakerRestTest {

    @Test
    void hent_fp_sak_roundtrip_test() {
        var aktørId = AktørId.dummy();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, AktørId::value), () -> aktørId);


        var uttaksperiodeDto1 = new FpSak.Uttaksperiode(LocalDate.now().minusWeeks(4), LocalDate.now().minusWeeks(2), new FpSak.Uttaksperiode.Resultat(
            FpSak.Uttaksperiode.Resultat.Type.INNVILGET));
        var uttaksperioder = List.of(uttaksperiodeDto1);
        var vedtak = new FpSak.Vedtak(LocalDateTime.now(), uttaksperioder, FpSak.Vedtak.Dekningsgrad.HUNDRE);

        var aktørIdAnnenPart = AktørId.dummy();
        var familieHendelse = new Sak.FamilieHendelse(LocalDate.now(), LocalDate.now().minusMonths(1), 1, null);
        var sakFraFpsak = new FpSak(Saksnummer.dummy().value(), aktørId.value(), familieHendelse, Set.of(vedtak), aktørIdAnnenPart.value());
        sendBehandlingHendelse(sakFraFpsak, repository);

        var sakerFraDBtilDto = tjeneste.hent().foreldrepenger().stream().toList();

        assertThat(sakerFraDBtilDto).hasSize(1);
        var sakFraDbOmgjortTilDto = sakerFraDBtilDto.get(0);
        assertThat(sakFraDbOmgjortTilDto.saksnummer().value()).isEqualTo(sakFraFpsak.saksnummer());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder()).hasSameSizeAs(vedtak.uttaksperioder());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).fom()).isEqualTo(vedtak.uttaksperioder().get(0).fom());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).tom()).isEqualTo(vedtak.uttaksperioder().get(0).tom());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).resultat().innvilget()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).fom()).isBefore(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).tom());
        assertThat(sakFraDbOmgjortTilDto.annenPart().fnr().value()).isEqualTo(aktørIdAnnenPart.value());
        assertThat(sakFraDbOmgjortTilDto.kanSøkeOmEndring()).isTrue();
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());
    }

    @Test
    void hent_svp_sak_roundtrip_test() {
        var aktørId = AktørId.dummy();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, AktørId::value), () -> aktørId);

        var familieHendelse = new Sak.FamilieHendelse(LocalDate.now(), LocalDate.now().minusMonths(1), 1, null);
        var sakFraFpsak = new SvpSak(Saksnummer.dummy().value(), aktørId.value(), familieHendelse);
        sendBehandlingHendelse(sakFraFpsak, repository);

        var sakerFraDBtilDto = tjeneste.hent().svangerskapspenger().stream().toList();

        assertThat(sakerFraDBtilDto).hasSize(1);
        var sakFraDbOmgjortTilDto = sakerFraDBtilDto.get(0);
        assertThat(sakFraDbOmgjortTilDto.saksnummer().value()).isEqualTo(sakFraFpsak.saksnummer());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());
    }

    @Test
    void hent_es_sak_roundtrip_test() {
        var aktørId = AktørId.dummy();
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new Saker(repository, AktørId::value), () -> aktørId);

        var familieHendelse = new Sak.FamilieHendelse(LocalDate.now(), LocalDate.now().minusMonths(1), 1, null);
        var sakFraFpsak = new EsSak(Saksnummer.dummy().value(), aktørId.value(), familieHendelse);
        sendBehandlingHendelse(sakFraFpsak, repository);

        var sakerFraDBtilDto = tjeneste.hent().engangsstønad().stream().toList();

        assertThat(sakerFraDBtilDto).hasSize(1);
        var sakFraDbOmgjortTilDto = sakerFraDBtilDto.get(0);
        assertThat(sakFraDbOmgjortTilDto.saksnummer().value()).isEqualTo(sakFraFpsak.saksnummer());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(sakFraDbOmgjortTilDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());
    }

    private static void sendBehandlingHendelse(Sak fraFpsak, RepositoryStub sakRepository) {
        var behandlingUuid = UUID.randomUUID();
        var prosessTaskData = opprettTask(behandlingUuid, UUID.randomUUID(), fraFpsak.saksnummer());
        new HentSakTask(new FpsakTjenesteStub(Map.of(behandlingUuid, fraFpsak)), sakRepository).doTask(prosessTaskData);
    }

}
