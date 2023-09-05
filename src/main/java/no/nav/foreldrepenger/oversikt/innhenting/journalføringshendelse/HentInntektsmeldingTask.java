package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.time.Duration;

@ApplicationScoped
@ProsessTask("hent.inntektsmelding")
public class HentInntektsmeldingTask implements ProsessTaskHandler {

    public static final Duration TASK_DELAY = Duration.ofMinutes(5);

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        // TODO
    }
}
