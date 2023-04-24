package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("test")
public class TestTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TestTask.class);
    static final String BEHANDLING_UUID = "behandlingUuid";

    private final FpSakKlient fpSakKlient;

    @Inject
    public TestTask(FpSakKlient fpSakKlient) {
        this.fpSakKlient = fpSakKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("kjører task");
        var behandlingUuid = UUID.fromString(prosessTaskData.getPropertyValue(BEHANDLING_UUID));
        var behandling = fpSakKlient.hentBehandling(behandlingUuid);
        LOG.info("Hentet behandling {} {}", behandlingUuid, behandling);
    }
}
