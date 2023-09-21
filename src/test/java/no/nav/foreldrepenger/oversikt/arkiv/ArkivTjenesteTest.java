package no.nav.foreldrepenger.oversikt.arkiv;

import static no.nav.foreldrepenger.oversikt.arkiv.ArkivDokumentDto.Type.INNGÅENDE_DOKUMENT;
import static no.nav.foreldrepenger.oversikt.arkiv.ArkivDokumentDto.Type.UTGÅENDE_DOKUMENT;
import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId.I000003;
import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId.I000023;
import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId.I000036;
import static no.nav.foreldrepenger.oversikt.innhenting.tidslinje.TidslinjeTjenesteTest.ettersender2Vedlegg;
import static no.nav.foreldrepenger.oversikt.innhenting.tidslinje.TidslinjeTjenesteTest.søknadMed1Vedlegg;
import static no.nav.foreldrepenger.oversikt.innhenting.tidslinje.TidslinjeTjenesteTest.utgåendeVedtak;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

class ArkivTjenesteTest {


    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private ArkivTjeneste arkivTjeneste;

    @BeforeEach
    void setUp() {
        dokumentArkivTjeneste = mock(DokumentArkivTjeneste.class);
        arkivTjeneste = new ArkivTjeneste(dokumentArkivTjeneste);
    }

    @Test
    void enJournalpostMed2DokumenterMappesTil2Arkivdokumenter() {
        var saksnummer = Saksnummer.dummy();
        var søknadMedVedlegg = søknadMed1Vedlegg(saksnummer, LocalDateTime.now());
        var dokumenterFraSøknad = søknadMedVedlegg.dokumenter();
        var dokument1 = dokumenterFraSøknad.get(0);
        var dokument2 = dokumenterFraSøknad.get(1);
        when(dokumentArkivTjeneste.hentAlleJournalposter(any())).thenReturn(List.of(søknadMedVedlegg));

        var dokumenter = arkivTjeneste.alle(saksnummer);

        assertThat(dokumenter).hasSameSizeAs(søknadMedVedlegg.dokumenter());
        assertThat(dokumenter)
            .extracting(ArkivDokumentDto::tittel)
            .containsExactly(dokument1.tittel(), dokument2.tittel());
        assertThat(dokumenter)
            .extracting(ArkivDokumentDto::dokumentId)
            .containsExactly(dokument1.dokumentId(), dokument2.dokumentId());

        for (var dokument : dokumenter) {
            assertThat(dokument.type()).isEqualTo(INNGÅENDE_DOKUMENT);
            assertThat(dokument.journalpostId()).isEqualTo(søknadMedVedlegg.journalpostId());
            assertThat(dokument.mottatt()).isEqualTo(søknadMedVedlegg.mottatt());
        }
    }



    @Test
    void søknadMed1VedlaggEttersending2VedleggOgVedtakProduserer5ArkivDokumenterMedKorrektTittel() {
        var saksnummer = Saksnummer.dummy();
        var tidspunkt = LocalDateTime.now().minusWeeks(4);
        var søknadMedVedleggSak1 = søknadMed1Vedlegg(saksnummer, tidspunkt);
        var søknadMedVedleggSak2 = ettersender2Vedlegg(saksnummer, tidspunkt.plusDays(1));
        var vedtak = utgåendeVedtak(saksnummer, tidspunkt.plusDays(2));
        when(dokumentArkivTjeneste.hentAlleJournalposter(saksnummer)).thenReturn(List.of(søknadMedVedleggSak1, søknadMedVedleggSak2, vedtak));

        var dokumenter = arkivTjeneste.alle(saksnummer);
        assertThat(dokumenter).hasSize(5);
        assertThat(dokumenter)
            .extracting(ArkivDokumentDto::type)
            .containsExactly(
                INNGÅENDE_DOKUMENT,
                INNGÅENDE_DOKUMENT,
                INNGÅENDE_DOKUMENT,
                INNGÅENDE_DOKUMENT,
                UTGÅENDE_DOKUMENT
            );
        assertThat(dokumenter)
            .extracting(ArkivDokumentDto::tittel)
            .containsExactly(
                I000003.getTittel(),
                I000036.getTittel(),
                I000023.getTittel(),
                I000036.getTittel(),
                vedtak.tittel()
            );
    }

    @Test
    void arkivDokumenteneSkalSorteresEtterMottattTidspunkt() {
        var saksnummer = Saksnummer.dummy();
        var tidspunkt = LocalDateTime.now().minusWeeks(4);
        var søknadMedVedleggSak1 = søknadMed1Vedlegg(saksnummer, tidspunkt);
        var vedtak = utgåendeVedtak(saksnummer, tidspunkt.plusDays(2));
        when(dokumentArkivTjeneste.hentAlleJournalposter(saksnummer)).thenReturn(List.of(vedtak, søknadMedVedleggSak1));

        var dokumenter = arkivTjeneste.alle(saksnummer);

        assertThat(dokumenter).hasSize(3);
        assertThat(dokumenter)
            .extracting(ArkivDokumentDto::type)
            .containsExactly(
                INNGÅENDE_DOKUMENT,
                INNGÅENDE_DOKUMENT,
                UTGÅENDE_DOKUMENT
            );
    }

}
