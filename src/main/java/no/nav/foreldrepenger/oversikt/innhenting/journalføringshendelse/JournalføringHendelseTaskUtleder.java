package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static no.nav.foreldrepenger.oversikt.oppgave.MinSideBeskjedVedMottattSøknadTask.AKTØRID;
import static no.nav.foreldrepenger.oversikt.oppgave.MinSideBeskjedVedMottattSøknadTask.ER_ENDRINGSSØKNAD;
import static no.nav.foreldrepenger.oversikt.oppgave.MinSideBeskjedVedMottattSøknadTask.EVENT_ID;
import static no.nav.foreldrepenger.oversikt.oppgave.MinSideBeskjedVedMottattSøknadTask.YTELSE_TYPE;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeHistoriske;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.foreldrepenger.oversikt.oppgave.MinSideBeskjedVedMottattSøknadTask;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
public class JournalføringHendelseTaskUtleder {
    private static final Logger LOG = LoggerFactory.getLogger(JournalføringHendelseTaskUtleder.class);
    private PersonOppslagSystem personOppslagSystem;

    @Inject
    JournalføringHendelseTaskUtleder(PersonOppslagSystem personOppslagSystem) {
        this.personOppslagSystem = personOppslagSystem;
    }

    JournalføringHendelseTaskUtleder() {
        // CDI
    }

    List<ProsessTaskData> utledProsesstask(EnkelJournalpost journalpost) {
        var saksnummer = journalpost.saksnummer();
        var dokumenttype = journalpost.hovedtype();
        if (dokumenttype == null || journalpost.erInfotrygdSak()) {
            return List.of();
        }

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
        m.setSekvens(genererNesteSekvens());
        m.setGruppe(saksnummer + "-V");
        return m;
    }

    private static String genererNesteSekvens() {
        //Prøver å hindre samme sekvens innad i samme taskgruppe
        return String.valueOf(Instant.now().toEpochMilli() + ThreadLocalRandom.current().nextLong(100L));
    }

    private ProsessTaskData lagHentInntektsmeldingTask(String saksnummer, String journalpostId) {
        var i = ProsessTaskData.forProsessTask(HentInntektsmeldingerTask.class);
        i.setSaksnummer(saksnummer);
        i.setProperty(HentInntektsmeldingerTask.JOURNALPOST_ID, journalpostId);
        i.medNesteKjøringEtter(LocalDateTime.now().plus(HentInntektsmeldingerTask.TASK_DELAY));
        i.setGruppe(saksnummer + "-I");
        i.setSekvens(genererNesteSekvens());
        return i;
    }

    private ProsessTaskData lagHentTilbakekrevingTask(String saksnummer) {
        var t = ProsessTaskData.forProsessTask(HentTilbakekrevingTask.class);
        t.setSaksnummer(saksnummer);
        t.setProperty(HentTilbakekrevingTask.FORVENTER_BESVART_VARSEL, "true");
        return t;
    }

    private ProsessTaskData lagSendBrukernotifikasjonBeskjedTask(String saksnummer, DokumentTypeHistoriske dokumentType, EnkelJournalpost journalpost) {
        var b = ProsessTaskData.forProsessTask(MinSideBeskjedVedMottattSøknadTask.class);
        b.setSaksnummer(saksnummer);
        b.setProperty(YTELSE_TYPE, gjelderYtelse(dokumentType).name());
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
        return personOppslagSystem.aktørId(new Fødselsnummer(bruker.ident()));
    }

    private static YtelseType gjelderYtelse(DokumentTypeHistoriske dokumentType) {
        return switch (dokumentType) {
            case I000001 -> YtelseType.SVANGERSKAPSPENGER;
            case I000002, I000005,  I000006, I000050 -> YtelseType.FORELDREPENGER;
            case I000003, I000004 -> YtelseType.ENGANGSSTØNAD;
            default -> throw new IllegalStateException("Ikke definert relevant ytelse for " + dokumentType.name());
        };
    }
}
