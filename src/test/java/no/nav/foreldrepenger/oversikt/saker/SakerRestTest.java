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

import no.nav.foreldrepenger.common.innsyn.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.HentSakTask;
import no.nav.foreldrepenger.oversikt.stub.FpsakTjenesteStub;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class SakerRestTest {

    @Test
    void hent_foreldrepenge_sak_roundtrip_test() {
        var aktørId = new AktørId( "aktørId");
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new FpSaker(repository), () -> aktørId);

        var uttaksperiodeDto1 = new FpsakTjeneste.SakDto.UttaksperiodeDto(LocalDate.now().minusWeeks(4), LocalDate.now().minusWeeks(2));
        var uttaksperioder = List.of(uttaksperiodeDto1);
        var uttak = new FpsakTjeneste.SakDto.UttakDto(Dekningsgrad.HUNDRE, uttaksperioder);
        var vedtak = new FpsakTjeneste.SakDto.VedtakDto(LocalDateTime.now(), uttak);
        var sakFraFpsak = new FpsakTjeneste.SakDto("123", aktørId.value(), Set.of(vedtak));
        sendBehandlingHendelse(sakFraFpsak, repository);

        var sakerFraDBtilDto = tjeneste.hent().foreldrepenger().stream().toList();

        assertThat(sakerFraDBtilDto).hasSize(1);
        var sakFraDbOmgjortTilDto = sakerFraDBtilDto.get(0);
        assertThat(sakFraDbOmgjortTilDto.saksnummer().value()).isEqualTo(sakFraFpsak.saksnummer());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder()).hasSameSizeAs(vedtak.uttak().perioder());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).fom()).isEqualTo(vedtak.uttak().perioder().get(0).fom());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).tom()).isEqualTo(vedtak.uttak().perioder().get(0).tom());
        assertThat(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).fom()).isBefore(sakFraDbOmgjortTilDto.gjeldendeVedtak().perioder().get(0).tom());

    }

    private static void sendBehandlingHendelse(FpsakTjeneste.SakDto fraFpsak, RepositoryStub sakRepository) {
        var behandlingUuid = UUID.randomUUID();
        var prosessTaskData = opprettTask(behandlingUuid, UUID.randomUUID());
        new HentSakTask(new FpsakTjenesteStub(Map.of(behandlingUuid, fraFpsak)), sakRepository).doTask(prosessTaskData);
    }

}
