package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import java.time.Instant;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

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
        if (dokumentType.erSøknad() || dokumentType.erVedlegg()) {
            var m = ProsessTaskData.forProsessTask(HentManglendeVedleggTask.class);
            m.setSaksnummer(saksnummer);
            m.setCallIdFraEksisterende();
            m.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
            m.setGruppe(saksnummer + "-V");
            prosessTaskTjeneste.lagre(m);
        }

        if (dokumentType.erInntektsmelding()) {
            var i = ProsessTaskData.forProsessTask(HentInntektsmeldingerTask.class);
            i.setSaksnummer(saksnummer);
            i.setProperty(HentInntektsmeldingerTask.JOURNALPOST_ID, journalpostId);
            i.setCallIdFraEksisterende();
            i.medNesteKjøringEtter(LocalDateTime.now().plus(HentInntektsmeldingerTask.TASK_DELAY));
            i.setGruppe(saksnummer + "-I");
            i.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
            prosessTaskTjeneste.lagre(i);
        }

        if (dokumentType.erUttalelseOmTilbakekreving() || erUtgåendeFraFptilbake(journalpost)) {
            var t = ProsessTaskData.forProsessTask(HentTilbakekrevingTask.class);
            t.setSaksnummer(saksnummer);
            t.setCallIdFraEksisterende();
            t.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
            t.setGruppe(HentTilbakekrevingTask.taskGruppeFor(saksnummer));
            t.medNesteKjøringEtter(LocalDateTime.now().plus(HentTilbakekrevingTask.TASK_DELAY));
            prosessTaskTjeneste.lagre(t);
        }
    }

    private static boolean erUtgåendeFraFptilbake(EnkelJournalpost journalpost) {
        return journalpost.journalposttype() == EnkelJournalpost.Journalposttype.UTGÅENDE
            && journalpost.kildeSystem() == EnkelJournalpost.KildeSystem.FPTILBAKE;
    }

}

