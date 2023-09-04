package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;

import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentInntektsmeldingTask.TASK_DELAY;

@ApplicationScoped
@ProsessTask("hendelse.hentFraJoark")
public class HentDataFraJoarkForHåndteringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentDataFraJoarkForHåndteringTask.class);
    public static final String JOURNALPOST_ID = "journalpostId";

    private final DokumentArkivTjeneste arkiv;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public HentDataFraJoarkForHåndteringTask(DokumentArkivTjeneste arkiv, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.arkiv = arkiv;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData p) {
        var journalpostId = p.getPropertyValue(JOURNALPOST_ID);
        var journalpostOpt = arkiv.hentJournalpost(new JournalpostId(journalpostId));

        if (journalpostOpt.isEmpty()) {
            LOG.info("Fant ikke journalpost med id {}", journalpostId);
            return;
        }

        var journalpost = journalpostOpt.get();
        var saksnummer = journalpost.saksnummer();
        var dokumentTypeOpt = journalpost.dokumentTypeId().stream().findFirst();

        if (dokumentTypeOpt.isEmpty() || saksnummer == null) {
            LOG.info("Journalpost uten dokumenttype [{}] og/eller saksnummer [{}]", dokumentTypeOpt.orElse(null), saksnummer);
            return;
        }

        var dokumentType = dokumentTypeOpt.get();
        if (dokumentType.erVedlegg()) {
            var m = ProsessTaskData.forProsessTask(HentMangledeVedleggTask.class);
            m.setSaksnummer(saksnummer);
            m.setCallIdFraEksisterende();
            prosessTaskTjeneste.lagre(m);
        }

        if (dokumentType.erInntektsmelding()) {
            var i = ProsessTaskData.forProsessTask(HentInntektsmeldingTask.class);
            i.setSaksnummer(saksnummer);
            i.setCallIdFraEksisterende();
            i.medNesteKjøringEtter(LocalDateTime.now().plus(TASK_DELAY));
            i.setGruppe(saksnummer);
            i.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
            prosessTaskTjeneste.lagre(i);
        }

        if (dokumentType.erTilbakekrevingUttalelse()) {
            var t = ProsessTaskData.forProsessTask(TilbakekrevingUttalelseMottattTask.class);
            t.setSaksnummer(saksnummer);
            t.setCallIdFraEksisterende();
            prosessTaskTjeneste.lagre(t);
        }
    }

}

