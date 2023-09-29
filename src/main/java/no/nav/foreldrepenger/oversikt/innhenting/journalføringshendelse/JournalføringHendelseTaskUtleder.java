package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask.AKTØRID;
import static no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask.ER_ENDRINGSSØKNAD;
import static no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask.EVENT_ID;
import static no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask.YTELSE_TYPE;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeId;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask;
import no.nav.foreldrepenger.oversikt.saker.AktørIdOppslag;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
public class JournalføringHendelseTaskUtleder {
    private static final Logger LOG = LoggerFactory.getLogger(JournalføringHendelseTaskUtleder.class);
    private AktørIdOppslag aktørIdOppslag;

    @Inject
    JournalføringHendelseTaskUtleder(AktørIdOppslag aktørIdOppslag) {
        this.aktørIdOppslag = aktørIdOppslag;
    }

    JournalføringHendelseTaskUtleder() {
        // CDI
    }

    List<ProsessTaskData> utledProsesstask(EnkelJournalpost journalpost) {
        var saksnummer = journalpost.saksnummer();
        var dokumenttype = journalpost.hovedtype();
        if (dokumenttype.erFørstegangssøknad() || dokumenttype.erEndringssøknad()) {
            return List.of(lagHentManglendeVedleggTask(saksnummer), lagSendBrukernotifikasjonBeskjedTask(saksnummer, dokumenttype, journalpost));
        } else if (dokumenttype.erVedlegg()) {
            return List.of(lagHentManglendeVedleggTask(saksnummer));
        } else if (dokumenttype.erInntektsmelding()) {
            return List.of(lagHentInntektsmeldingTask(saksnummer, journalpost.journalpostId()));
        } else if (dokumenttype.erUttalelseOmTilbakekreving()) {
            return List.of(lagHentTilbakekrevingTask(saksnummer));
        } else {
            LOG.info("Journalføringshendelse av dokumenttypen {} på sak {} ignoreres", dokumenttype, saksnummer);
            return List.of();
        }
    }

    private ProsessTaskData lagHentManglendeVedleggTask(String saksnummer) {
        var m = ProsessTaskData.forProsessTask(HentManglendeVedleggTask.class);
        m.setSaksnummer(saksnummer);
        m.setCallIdFraEksisterende();
        m.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        m.setGruppe(saksnummer + "-V");
        return m;
    }

    private ProsessTaskData lagHentInntektsmeldingTask(String saksnummer, String journalpostId) {
        var i = ProsessTaskData.forProsessTask(HentInntektsmeldingerTask.class);
        i.setSaksnummer(saksnummer);
        i.setProperty(HentInntektsmeldingerTask.JOURNALPOST_ID, journalpostId);
        i.setCallIdFraEksisterende();
        i.medNesteKjøringEtter(LocalDateTime.now().plus(HentInntektsmeldingerTask.TASK_DELAY));
        i.setGruppe(saksnummer + "-I");
        i.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return i;
    }

    private ProsessTaskData lagHentTilbakekrevingTask(String saksnummer) {
        var t = ProsessTaskData.forProsessTask(HentTilbakekrevingTask.class);
        t.setSaksnummer(saksnummer);
        t.setCallIdFraEksisterende();
        t.setProperty(HentTilbakekrevingTask.FORVENTER_BESVART_VARSEL, "true");
        return t;
    }

    private ProsessTaskData lagSendBrukernotifikasjonBeskjedTask(String saksnummer, DokumentTypeId dokumentType, EnkelJournalpost journalpost) {
        var b = ProsessTaskData.forProsessTask(BrukernotifikasjonBeskjedVedMottattSøknadTask.class);
        b.setSaksnummer(saksnummer);
        b.setCallIdFraEksisterende();
        b.setProperty(YTELSE_TYPE, dokumentType.gjelderYtelse().name());
        b.setProperty(ER_ENDRINGSSØKNAD, Boolean.toString(dokumentType.erEndringssøknad()));
        b.setProperty(AKTØRID, tilAktørid(journalpost.bruker()).value());
        b.setProperty(EVENT_ID, kanalreferanseTilUuid(journalpost.eksternReferanse()).toString());
        return b;
    }

    private static UUID kanalreferanseTilUuid(String kanalreferanse) {
        try {
            return UUID.fromString(kanalreferanse);
        } catch (NullPointerException | IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }

    private AktørId tilAktørid(EnkelJournalpost.Bruker bruker) {
        if (EnkelJournalpost.Bruker.Type.AKTØRID.equals(bruker.type())) {
            return new AktørId(bruker.ident());
        }
        return aktørIdOppslag.forFnr(new Fødselsnummer(bruker.ident()));
    }
}
