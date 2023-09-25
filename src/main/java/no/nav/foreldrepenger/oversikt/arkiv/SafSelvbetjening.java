package no.nav.foreldrepenger.oversikt.arkiv;

import no.nav.safselvbetjening.Dokumentoversikt;
import no.nav.safselvbetjening.DokumentoversiktResponseProjection;
import no.nav.safselvbetjening.DokumentoversiktSelvbetjeningQueryRequest;

import java.net.http.HttpResponse;

public interface SafSelvbetjening {
    HttpResponse<byte[]> dokument(JournalpostId journalpostId, DokumentId dokumentId);
    Dokumentoversikt dokumentoversiktSelvbetjening(DokumentoversiktSelvbetjeningQueryRequest q, DokumentoversiktResponseProjection p);
}
