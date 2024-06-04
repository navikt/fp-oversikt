package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "dittnav.opprett", prioritet = 2)
class MinSideOpprettOppgaveTask implements ProsessTaskHandler {

    static final String OPPGAVE_ID = "oppgaveId";
    private final MinSideTjeneste minSideTjeneste;
    private final OppgaveRepository oppgaveRepository;

    @Inject
    MinSideOpprettOppgaveTask(MinSideTjeneste minSideTjeneste, OppgaveRepository oppgaveRepository) {
        this.minSideTjeneste = minSideTjeneste;
        this.oppgaveRepository = oppgaveRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var oppgaveId = UUID.fromString(prosessTaskData.getPropertyValue(OPPGAVE_ID));
        var oppgave = oppgaveRepository.hent(oppgaveId);
        minSideTjeneste.opprett(oppgave);
    }
}
