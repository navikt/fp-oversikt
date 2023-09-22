package no.nav.foreldrepenger.oversikt.arkiv;

import static no.nav.foreldrepenger.oversikt.arkiv.SafTjeneste.FP_DOK_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;
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
        var dokumentTypeId = DokumentTypeId.I000001;
        var journalførtSøknad = journalførtSøknad(dokumentTypeId);
        when(saf.hentJournalpostInfo(any(), any())).thenReturn(journalførtSøknad);

        var journalpostOpt = arkivTjeneste.hentJournalpostUtenDokument(new JournalpostId("123"));

        assertThat(journalpostOpt).isPresent();
        var journalpost = journalpostOpt.get();
        assertThat(journalpost.tittel()).isEqualTo(journalførtSøknad.getTittel());
        assertThat(journalpost.journalpostId()).isEqualTo(journalførtSøknad.getJournalpostId());
        assertThat(journalpost.saksnummer()).isEqualTo(journalførtSøknad.getSak().getFagsakId());
        assertThat(journalpost.bruker().id()).isEqualTo(journalførtSøknad.getBruker().getId());
        assertThat(journalpost.type()).isNotNull();
        assertThat(journalpost.mottatt()).isNotNull();
        assertThat(journalpost.hovedtype().name()).isEqualTo(dokumentTypeId.name());
        assertThat(journalpost.dokumenter()).isEmpty();
    }

    private static Journalpost notat() {
        var journalførtNotat = new Journalpost();
        journalførtNotat.setJournalposttype(Journalposttype.N);
        return journalførtNotat;
    }

    private static Journalpost journalførtSøknad(DokumentTypeId dokumentTypeId) {
        var journalpost = new Journalpost();
        journalpost.setJournalposttype(Journalposttype.I);
        journalpost.setJournalstatus(Journalstatus.MOTTATT);
        journalpost.setSkjerming("FEIL");
        journalpost.setTittel(dokumentTypeId.getTittel());
        journalpost.setTilleggsopplysninger(List.of(new Tilleggsopplysning(FP_DOK_TYPE, dokumentTypeId.name())));
        journalpost.setJournalpostId("123");
        var sak = new Sak();
        sak.setFagsakId(Saksnummer.dummy().value());
        journalpost.setSak(sak);
        journalpost.setBruker(new Bruker(AktørId.dummy().value(), BrukerIdType.AKTOERID));
        journalpost.setDatoOpprettet(Date.from(Instant.now()));
        journalpost.setBehandlingstema("ab000123");
        journalpost.setDokumenter(List.of(pdfDokument(dokumentTypeId), xmlDokument(dokumentTypeId)));
        return journalpost;
    }

    private static Journalpost gammelJournalførtSøknadUtenDokumenttypidITilleggsinfo(DokumentTypeId dokumentTypeId) {
        var journalpost = new Journalpost();
        journalpost.setJournalposttype(Journalposttype.I);
        journalpost.setJournalstatus(Journalstatus.MOTTATT);
        journalpost.setSkjerming("FEIL");
        journalpost.setTittel(dokumentTypeId.getTittel());
        journalpost.setJournalpostId("123");
        var sak = new Sak();
        sak.setFagsakId(Saksnummer.dummy().value());
        journalpost.setSak(sak);
        journalpost.setBruker(new Bruker(AktørId.dummy().value(), BrukerIdType.AKTOERID));
        journalpost.setDatoOpprettet(Date.from(Instant.now()));
        journalpost.setBehandlingstema("ab000123");
        journalpost.setDokumenter(List.of(pdfDokument(dokumentTypeId), xmlDokument(dokumentTypeId)));
        return journalpost;
    }

    private static Journalpost journalpostUgyldigTittelUtenTilleggsinfoMenRiktigDokumentTittel(DokumentTypeId dokumentTypeId) {
        var journalpost = new Journalpost();
        journalpost.setJournalposttype(Journalposttype.I);
        journalpost.setJournalstatus(Journalstatus.MOTTATT);
        journalpost.setSkjerming("FEIL");
        journalpost.setTittel("FEIL_TITTEL");
        journalpost.setJournalpostId("123");
        var sak = new Sak();
        sak.setFagsakId(Saksnummer.dummy().value());
        journalpost.setSak(sak);
        journalpost.setBruker(new Bruker(AktørId.dummy().value(), BrukerIdType.AKTOERID));
        journalpost.setDatoOpprettet(Date.from(Instant.now()));
        journalpost.setBehandlingstema("ab000123");
        journalpost.setDokumenter(List.of(pdfDokument(dokumentTypeId), xmlDokument(dokumentTypeId)));
        return journalpost;
    }

    private static Journalpost journalførtEttersending() {
        var journalpost = new Journalpost();
        journalpost.setJournalposttype(Journalposttype.I);
        journalpost.setJournalstatus(Journalstatus.MOTTATT);
        journalpost.setSkjerming("FEIL");
        var dokumentType = DokumentTypeId.I000042;
        journalpost.setTittel(dokumentType.getTittel());
        journalpost.setTilleggsopplysninger(List.of(new Tilleggsopplysning(FP_DOK_TYPE, dokumentType.name())));
        journalpost.setJournalpostId("123");
        var sak = new Sak();
        sak.setFagsakId(Saksnummer.dummy().value());
        journalpost.setSak(sak);
        journalpost.setBruker(new Bruker(AktørId.dummy().value(), BrukerIdType.AKTOERID));
        journalpost.setDatoOpprettet(Date.from(Instant.now()));
        journalpost.setBehandlingstema("ab000123");
        journalpost.setDokumenter(List.of(pdfDokument(dokumentType), pdfDokument(DokumentTypeId.I000045)));
        return journalpost;
    }

    private static Journalpost journalførtVedtak() {
        var journalpost = new Journalpost();
        journalpost.setJournalposttype(Journalposttype.U);
        journalpost.setJournalstatus(Journalstatus.EKSPEDERT);
        journalpost.setSkjerming("FEIL");
        journalpost.setTittel("Vedtak");
        journalpost.setJournalpostId("123");
        var sak = new Sak();
        sak.setFagsakId(Saksnummer.dummy().value());
        journalpost.setSak(sak);
        journalpost.setBruker(new Bruker(AktørId.dummy().value(), BrukerIdType.AKTOERID));
        journalpost.setDatoOpprettet(Date.from(Instant.now()));
        journalpost.setBehandlingstema("ab000123");
        journalpost.setDokumenter(List.of(pdfDokument("INVFOR")));
        return journalpost;
    }

    private static Journalpost journalførtDokumentBareXML() {
        var journalpost = new Journalpost();
        journalpost.setJournalposttype(Journalposttype.I);
        journalpost.setJournalstatus(Journalstatus.MOTTATT);
        journalpost.setSkjerming("FEIL");
        var dokumentType = DokumentTypeId.I000060;
        journalpost.setTittel(dokumentType.getTittel());
        journalpost.setTilleggsopplysninger(List.of(new Tilleggsopplysning(FP_DOK_TYPE, dokumentType.name())));
        journalpost.setJournalpostId("123");
        var sak = new Sak();
        sak.setFagsakId(Saksnummer.dummy().value());
        journalpost.setSak(sak);
        journalpost.setBruker(new Bruker(AktørId.dummy().value(), BrukerIdType.AKTOERID));
        journalpost.setDatoOpprettet(Date.from(Instant.now()));
        journalpost.setBehandlingstema("ab000123");
        journalpost.setDokumenter(List.of(xmlDokument(dokumentType)));
        return journalpost;
    }

    private static DokumentInfo pdfDokument(String brevKode) {
        var dokument = new DokumentInfo();
        dokument.setDokumentInfoId("123");
        dokument.setTittel("Innvilgelsesbrev Foreldrepenger");
        dokument.setBrevkode(brevKode);
        var dokumentvariant = new Dokumentvariant();
        dokumentvariant.setFiltype("PDF");
        dokument.setDokumentvarianter(List.of(dokumentvariant));
        return dokument;
    }

    private static DokumentInfo pdfDokument(DokumentTypeId dokumentTypeId) {
        var dokument = new DokumentInfo();
        dokument.setDokumentInfoId("123");
        dokument.setTittel(dokumentTypeId.getTittel());
        dokument.setBrevkode(null);
        var dokumentvariant = new Dokumentvariant();
        dokumentvariant.setFiltype("PDF");
        dokument.setDokumentvarianter(List.of(dokumentvariant));
        return dokument;
    }

    private static DokumentInfo xmlDokument(DokumentTypeId dokumentTypeId) {
        var dokument = new DokumentInfo();
        dokument.setDokumentInfoId("456");
        dokument.setTittel(dokumentTypeId.getTittel());
        dokument.setBrevkode(null);
        var dokumentvariant = new Dokumentvariant();
        dokumentvariant.setFiltype("XML");
        dokument.setDokumentvarianter(List.of(dokumentvariant));
        return dokument;
    }

}