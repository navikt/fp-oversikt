package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.time.Duration;

public class HentInntektsmeldingTask implements ProsessTaskHandler {

    public static final Duration TASK_DELAY = Duration.ofMinutes(5);

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        // TODO
    }
}
