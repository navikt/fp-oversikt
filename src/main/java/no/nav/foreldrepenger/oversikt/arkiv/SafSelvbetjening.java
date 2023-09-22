package no.nav.foreldrepenger.oversikt.arkiv;

import no.nav.safselvbetjening.Dokumentoversikt;
import no.nav.safselvbetjening.DokumentoversiktResponseProjection;
import no.nav.safselvbetjening.DokumentoversiktSelvbetjeningQueryRequest;

public interface SafSelvbetjening {
    Dokumentoversikt dokumentoversiktSelvbetjening(DokumentoversiktSelvbetjeningQueryRequest q, DokumentoversiktResponseProjection p);
}
