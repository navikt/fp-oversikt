package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hent.uttalelseTilbakekreving")
public class TilbakekrevingUttalelseMottattTask implements ProsessTaskHandler {

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        // TODO
    }
}
