package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostType;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.oppgave.MinSideBeskjedVedMottattSøknadTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenpartUbeskyttetAdresse;
import static org.assertj.core.api.Assertions.assertThat;

class JournalføringHendelseTaskUtlederTest {

    private JournalføringHendelseTaskUtleder journalføringHendelseTaskUtleder;

    @BeforeEach
    void setUp() {
        journalføringHendelseTaskUtleder = new JournalføringHendelseTaskUtleder(annenpartUbeskyttetAdresse());
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
                MinSideBeskjedVedMottattSøknadTask.TASK_TYPE
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
            "FS36",
            UUID.randomUUID().toString(),
            Saksnummer.dummy().value(),
            JournalpostType.INNGÅENDE_DOKUMENT,
            new EnkelJournalpost.Bruker(EnkelJournalpost.Bruker.Type.AKTØRID, AktørId.dummy().value()),
            DokumentType.I000001
        );
    }

    private static EnkelJournalpost ettersending() {
        return new EnkelJournalpost(
            "123",
            "FS36",
            UUID.randomUUID().toString(),
            Saksnummer.dummy().value(),
            JournalpostType.INNGÅENDE_DOKUMENT,
            new EnkelJournalpost.Bruker(EnkelJournalpost.Bruker.Type.FNR, "000000"),
            DokumentType.I000038
        );
    }

    private static EnkelJournalpost inntektsmelding() {
        return new EnkelJournalpost(
            "123",
            "FS36",
            UUID.randomUUID().toString(),
            Saksnummer.dummy().value(),
            JournalpostType.INNGÅENDE_DOKUMENT,
            new EnkelJournalpost.Bruker(EnkelJournalpost.Bruker.Type.AKTØRID, "000000"),
            DokumentType.I000067
        );
    }

    private static EnkelJournalpost uttalelseTilbakebetaling() {
        return new EnkelJournalpost(
            "123",
            "FS36",
            UUID.randomUUID().toString(),
            Saksnummer.dummy().value(),
            JournalpostType.INNGÅENDE_DOKUMENT,
            new EnkelJournalpost.Bruker(EnkelJournalpost.Bruker.Type.FNR, "000000"),
            DokumentType.I000114
        );
    }
}
