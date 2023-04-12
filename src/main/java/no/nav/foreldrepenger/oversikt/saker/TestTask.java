package no.nav.foreldrepenger.oversikt.saker;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("test")
public class TestTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TestTask.class);

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("kjører task");
    }
}
