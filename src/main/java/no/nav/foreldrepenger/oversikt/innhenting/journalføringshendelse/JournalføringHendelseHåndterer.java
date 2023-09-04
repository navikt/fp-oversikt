package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentDataFraJoarkForHåndteringTask.JOURNALPOST_ID;


@ApplicationScoped
@ActivateRequestContext
@Transactional
public class JournalføringHendelseHåndterer {

    private ProsessTaskTjeneste taskTjeneste;

    JournalføringHendelseHåndterer() {
        // CDI
    }

    @Inject
    public JournalføringHendelseHåndterer(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    void handleMessage(JournalfoeringHendelseRecord payload) {
        var journalpostId = String.valueOf(payload.getJournalpostId());
        var kanalreferanse = payload.getKanalReferanseId();
        lagreHentMangledeVedleggTask(journalpostId, kanalreferanse);
    }

    private void lagreHentMangledeVedleggTask(String journalpostId, String kanalreferanse) {
        var task = ProsessTaskData.forProsessTask(HentDataFraJoarkForHåndteringTask.class);
        task.setCallId(kanalreferanse);
        task.setProperty(JOURNALPOST_ID, journalpostId);
        taskTjeneste.lagre(task);
    }
}
