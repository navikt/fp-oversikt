package no.nav.foreldrepenger.oversikt.arkiv;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.saf.AvsenderMottaker;
import no.nav.saf.AvsenderMottakerIdType;
import no.nav.saf.Bruker;
import no.nav.saf.BrukerIdType;
import no.nav.saf.DokumentInfo;
import no.nav.saf.Dokumentvariant;
import no.nav.saf.Journalpost;
import no.nav.saf.Journalposttype;
import no.nav.saf.Journalstatus;
import no.nav.saf.Sak;
import no.nav.saf.Tilleggsopplysning;
import no.nav.vedtak.felles.integrasjon.saf.Saf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static no.nav.foreldrepenger.oversikt.arkiv.SafTjeneste.FP_DOK_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SafTjenesteTest {

    private Saf saf;
    private SafTjeneste arkivTjeneste;

    @BeforeEach
    void setUp() {
        saf = mock(Saf.class);
        arkivTjeneste = new SafTjeneste(saf);
    }

    @Test
    void verifiserInnholdMappesKorrektForJournalpost() {
        var dokumentTypeId = DokumentType.I000001;
        var journalførtSøknad = journalførtSøknad(dokumentTypeId);
        when(saf.hentJournalpostInfo(any(), any())).thenReturn(journalførtSøknad);

        var journalpostOpt = arkivTjeneste.hentJournalpostUtenDokument(new JournalpostId("123"));

        assertThat(journalpostOpt).isPresent();
        var journalpost = journalpostOpt.get();
        assertThat(journalpost.saksnummer()).isEqualTo(journalførtSøknad.getSak().getFagsakId());
        assertThat(journalpost.type()).isNotNull();
        assertThat(journalpost.hovedtype().name()).isEqualTo(dokumentTypeId.name());
    }

    @Test
    void verifiserAtDokumentIdUtledesFraTittelVedManglendeTilleggopplysningsfelt() {
        var dokumentTypeId = DokumentType.I000001;
        var journalpostUtenTilleggsopplysninger = journalpostUtenTilleggsopplysninger(dokumentTypeId);
        when(saf.hentJournalpostInfo(any(), any())).thenReturn(journalpostUtenTilleggsopplysninger);

        var journalpostOpt = arkivTjeneste.hentJournalpostUtenDokument(new JournalpostId("123"));

        assertThat(journalpostOpt).isPresent();
        var journalpost = journalpostOpt.get();
        assertThat(journalpost.hovedtype()).isEqualTo(dokumentTypeId);
    }


    private static Journalpost journalpostUtenTilleggsopplysninger(DokumentType dokumentTypeId) {
        var journalpost = new Journalpost();
        journalpost.setJournalposttype(Journalposttype.I);
        journalpost.setJournalstatus(Journalstatus.MOTTATT);
        journalpost.setSkjerming("FEIL");
        journalpost.setTittel(dokumentTypeId.getTittel());
        journalpost.setTilleggsopplysninger(List.of());
        journalpost.setJournalpostId("123");
        var avsenderMottaker = new AvsenderMottaker("12345678901", AvsenderMottakerIdType.FNR, null, null, true);
        journalpost.setAvsenderMottaker(avsenderMottaker);
        var sak = new Sak();
        sak.setFagsakId(Saksnummer.dummy().value());
        journalpost.setSak(sak);
        journalpost.setBruker(new Bruker(AktørId.dummy().value(), BrukerIdType.AKTOERID));
        journalpost.setDatoOpprettet(Date.from(Instant.now()));
        journalpost.setBehandlingstema("ab000123");
        journalpost.setDokumenter(List.of(pdfDokument(dokumentTypeId), xmlDokument(dokumentTypeId)));
        return journalpost;
    }

    private static Journalpost journalførtSøknad(DokumentType dokumentTypeId) {
        var journalpost = new Journalpost();
        journalpost.setJournalposttype(Journalposttype.I);
        journalpost.setJournalstatus(Journalstatus.MOTTATT);
        journalpost.setSkjerming("FEIL");
        journalpost.setTittel(dokumentTypeId.getTittel());
        journalpost.setTilleggsopplysninger(List.of(new Tilleggsopplysning(FP_DOK_TYPE, dokumentTypeId.name())));
        journalpost.setJournalpostId("123");
        var avsenderMottaker = new AvsenderMottaker("12345678901", AvsenderMottakerIdType.FNR, null, null, true);
        journalpost.setAvsenderMottaker(avsenderMottaker);
        var sak = new Sak();
        sak.setFagsakId(Saksnummer.dummy().value());
        journalpost.setSak(sak);
        journalpost.setBruker(new Bruker(AktørId.dummy().value(), BrukerIdType.AKTOERID));
        journalpost.setDatoOpprettet(Date.from(Instant.now()));
        journalpost.setBehandlingstema("ab000123");
        journalpost.setDokumenter(List.of(pdfDokument(dokumentTypeId), xmlDokument(dokumentTypeId)));
        return journalpost;
    }

    private static DokumentInfo pdfDokument(DokumentType dokumentTypeId) {
        var dokument = new DokumentInfo();
        dokument.setDokumentInfoId("123");
        dokument.setTittel(dokumentTypeId.getTittel());
        dokument.setBrevkode(null);
        var dokumentvariant = new Dokumentvariant();
        dokument.setDokumentvarianter(List.of(dokumentvariant));
        return dokument;
    }

    private static DokumentInfo xmlDokument(DokumentType dokumentTypeId) {
        var dokument = new DokumentInfo();
        dokument.setDokumentInfoId("456");
        dokument.setTittel(dokumentTypeId.getTittel());
        dokument.setBrevkode(null);
        var dokumentvariant = new Dokumentvariant();
        dokument.setDokumentvarianter(List.of(dokumentvariant));
        return dokument;
    }

}
