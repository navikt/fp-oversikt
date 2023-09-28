package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import java.time.Instant;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost;

import no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.arkiv.SafTjeneste;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import static no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask.ER_ENDRINGSSØKNAD;
import static no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask.FØDSELSNUMMER;
import static no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask.YTELSE_TYPE;
import static no.nav.vedtak.felles.prosesstask.api.CommonTaskProperties.SAKSNUMMER;

@ApplicationScoped
@ProsessTask("hendelse.hentFraJoark")
public class HentDataFraJoarkForHåndteringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentDataFraJoarkForHåndteringTask.class);
    public static final String JOURNALPOST_ID = "journalpostId";

    private final SafTjeneste arkiv;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public HentDataFraJoarkForHåndteringTask(SafTjeneste arkiv, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.arkiv = arkiv;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
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
        var dokumentType = journalpost.hovedtype();

        if (saksnummer == null) {
            LOG.info("Journalpost saksnummer [{}]", saksnummer);
            return;
        }

        if (dokumentType.erFørstegangssøknad() || dokumentType.erEndringssøknad() || dokumentType.erVedlegg()) {
            sendMinSideBeskjed(journalpost);
            var m = ProsessTaskData.forProsessTask(HentManglendeVedleggTask.class);
            m.setSaksnummer(saksnummer);
            m.setCallIdFraEksisterende();
            m.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
            m.setGruppe(saksnummer + "-V");
            prosessTaskTjeneste.lagre(m);
        } else if (dokumentType.erInntektsmelding()) {
            var i = ProsessTaskData.forProsessTask(HentInntektsmeldingerTask.class);
            i.setSaksnummer(saksnummer);
            i.setProperty(HentInntektsmeldingerTask.JOURNALPOST_ID, journalpostId);
            i.setCallIdFraEksisterende();
            i.medNesteKjøringEtter(LocalDateTime.now().plus(HentInntektsmeldingerTask.TASK_DELAY));
            i.setGruppe(saksnummer + "-I");
            i.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
            prosessTaskTjeneste.lagre(i);
        } else if (dokumentType.erUttalelseOmTilbakekreving()) {
            var t = ProsessTaskData.forProsessTask(HentTilbakekrevingTask.class);
            t.setSaksnummer(saksnummer);
            t.setCallIdFraEksisterende();
            t.setProperty(HentTilbakekrevingTask.FORVENTER_BESVART_VARSEL, "true");
            //Setter ikke gruppe/seksvens for unngå at tasken blokkeres av task som venter at varselbrev sendes
            prosessTaskTjeneste.lagre(t);
        } else {
            LOG.info("Journalføringshendelse av dokumenttypen {} på sak {} ignoreres", dokumentType, saksnummer);
        }
    }

    private void sendMinSideBeskjed(EnkelJournalpost journalpost) {
        var saksnummer = journalpost.saksnummer();
        var fødselsnummer = journalpost.fødselsnummerAvsenderMottaker().value();
        var dokumentTypeHoveddokument = journalpost.hovedtype();
        if (dokumentTypeHoveddokument.erFørstegangssøknad() || dokumentTypeHoveddokument.erEndringssøknad()) {
            var ytelseType = dokumentTypeHoveddokument.gjelderYtelse();
            var erEndringssøknad = dokumentTypeHoveddokument.erEndringssøknad();
            var task = ProsessTaskData.forProsessTask(BrukernotifikasjonBeskjedVedMottattSøknadTask.class);
            task.setSaksnummer(saksnummer);
            task.setCallIdFraEksisterende();
            task.setProperty(YTELSE_TYPE, ytelseType.name());
            task.setProperty(ER_ENDRINGSSØKNAD, Boolean.toString(erEndringssøknad));
            task.setProperty(FØDSELSNUMMER, fødselsnummer);
            task.setProperty(SAKSNUMMER, saksnummer);
            prosessTaskTjeneste.lagre(task);
        }
    }
}

