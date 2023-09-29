package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeId;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

class JournalføringHendelseTaskUtlederTest {

    private JournalføringHendelseTaskUtleder journalføringHendelseTaskUtleder;

    @BeforeEach
    void setUp() {
        journalføringHendelseTaskUtleder = new JournalføringHendelseTaskUtleder(fnr -> AktørId.dummy());
    }

    @Test
    void søknadSkalResultereIHentManglendeVedleggTaskOgBrukerNotifikasjonTask() {
        var prosessTaskData = journalføringHendelseTaskUtleder.utledProsesstask(søknad());

        assertThat(prosessTaskData)
            .hasSize(2)
            .extracting(ProsessTaskData::taskType)
            .extracting(TaskType::value)
            .containsExactly(
                HentManglendeVedleggTask.TASK_TYPE,
                BrukernotifikasjonBeskjedVedMottattSøknadTask.TASK_TYPE
            );
    }

    @Test
    void ettersendingSkalResultereIBareHentManglendeVedleggTask() {
        var prosessTaskData = journalføringHendelseTaskUtleder.utledProsesstask(ettersending());

        assertThat(prosessTaskData)
            .hasSize(1)
            .extracting(ProsessTaskData::taskType)
            .extracting(TaskType::value)
            .containsExactly(
                HentManglendeVedleggTask.TASK_TYPE
            );
    }

    @Test
    void inntektsmeldingSkalResultereIBareHentManglendeVedleggTask() {
        var prosessTaskData = journalføringHendelseTaskUtleder.utledProsesstask(inntektsmelding());

        assertThat(prosessTaskData)
            .hasSize(1)
            .extracting(ProsessTaskData::taskType)
            .extracting(TaskType::value)
            .containsExactly(
                HentInntektsmeldingerTask.TASK_TYPE
            );
    }

    @Test
    void uttalelseTilbakebetalingSkalResultereIBareHentTilbakekrevingTask() {
        var prosessTaskData = journalføringHendelseTaskUtleder.utledProsesstask(uttalelseTilbakebetaling());

        assertThat(prosessTaskData)
            .hasSize(1)
            .extracting(ProsessTaskData::taskType)
            .extracting(TaskType::value)
            .containsExactly(
                HentTilbakekrevingTask.TASK_TYPE
            );
    }

    private static EnkelJournalpost søknad() {
        return new EnkelJournalpost(
            "123",
            UUID.randomUUID().toString(),
            Saksnummer.dummy().value(),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            new EnkelJournalpost.Bruker(EnkelJournalpost.Bruker.Type.AKTØRID, AktørId.dummy().value()),
            DokumentTypeId.I000001
        );
    }

    private static EnkelJournalpost ettersending() {
        return new EnkelJournalpost(
            "123",
            UUID.randomUUID().toString(),
            Saksnummer.dummy().value(),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            new EnkelJournalpost.Bruker(EnkelJournalpost.Bruker.Type.FNR, "000000"),
            DokumentTypeId.I000038
        );
    }

    private static EnkelJournalpost inntektsmelding() {
        return new EnkelJournalpost(
            "123",
            UUID.randomUUID().toString(),
            Saksnummer.dummy().value(),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            new EnkelJournalpost.Bruker(EnkelJournalpost.Bruker.Type.AKTØRID, "000000"),
            DokumentTypeId.I000067
        );
    }

    private static EnkelJournalpost uttalelseTilbakebetaling() {
        return new EnkelJournalpost(
            "123",
            UUID.randomUUID().toString(),
            Saksnummer.dummy().value(),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            new EnkelJournalpost.Bruker(EnkelJournalpost.Bruker.Type.FNR, "000000"),
            DokumentTypeId.I000114
        );
    }
}
