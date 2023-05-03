package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.innhenting.BehandlingHendelseHåndterer.opprettTask;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.HentSakTask;
import no.nav.foreldrepenger.oversikt.stub.FpsakTjenesteStub;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class SakerRestTest {

    @Test
    void hent_foreldrepenge_sak() {
        var aktørId = "aktørId";
        var repository = new RepositoryStub();
        var tjeneste = new SakerRest(new FpSaker(repository), () -> aktørId);

        var fraFpsak = new FpsakTjeneste.SakDto("123", FpsakTjeneste.SakDto.Status.ÅPEN, FpsakTjeneste.SakDto.YtelseType.FORELDREPENGER, aktørId);
        sendBehandlingHendelse(fraFpsak, repository);

        var saker = tjeneste.hent().foreldrepenger().stream().toList();

        assertThat(saker).hasSize(1);
        assertThat(saker.get(0).saksnummer().value()).isEqualTo(fraFpsak.saksnummer());
    }

    private static void sendBehandlingHendelse(FpsakTjeneste.SakDto fraFpsak, RepositoryStub vedtakRepository) {
        var behandlingUuid = UUID.randomUUID();
        var prosessTaskData = opprettTask(behandlingUuid, UUID.randomUUID());
        new HentSakTask(new FpsakTjenesteStub(Map.of(behandlingUuid, fraFpsak)), vedtakRepository).doTask(prosessTaskData);
    }

}
