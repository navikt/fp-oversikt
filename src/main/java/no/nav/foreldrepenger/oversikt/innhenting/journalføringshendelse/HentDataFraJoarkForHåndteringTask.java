package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostId;
import no.nav.foreldrepenger.oversikt.arkiv.SafTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "hendelse.hentFraJoark", prioritet = 2)
public class HentDataFraJoarkForHåndteringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentDataFraJoarkForHåndteringTask.class);
    public static final String JOURNALPOST_ID = "journalpostId";

    private final SafTjeneste arkiv;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final JournalføringHendelseTaskUtleder journalføringHendelseTaskUtleder;

    @Inject
    public HentDataFraJoarkForHåndteringTask(SafTjeneste arkiv, ProsessTaskTjeneste prosessTaskTjeneste, JournalføringHendelseTaskUtleder journalføringHendelseTaskUtleder) {
        this.arkiv = arkiv;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.journalføringHendelseTaskUtleder = journalføringHendelseTaskUtleder;
    }

    @Override
    public void doTask(ProsessTaskData p) {
        var journalpostId = p.getPropertyValue(JOURNALPOST_ID);
        var journalpostOpt = arkiv.hentJournalpostUtenDokument(new JournalpostId(journalpostId));

        if (journalpostOpt.isEmpty()) {
            LOG.info("Fant ikke journalpost med id {}", journalpostId);
            return;
        }

        var journalpost = journalpostOpt.get();
        var saksnummer = journalpost.saksnummer();

        if (saksnummer == null) {
            LOG.info("Journalpost uten saksnummer for journalpost {}", journalpost);
            return;
        }

        var prosesstask = journalføringHendelseTaskUtleder.utledProsesstask(journalpost);
        lagreProsesstask(prosesstask);
    }

    private void lagreProsesstask(List<ProsessTaskData> prosesstask) {
        for (var task : prosesstask) {
            prosessTaskTjeneste.lagre(task);
        }
    }

}

