package no.nav.foreldrepenger.oversikt.saker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

class TilgangsstyringBorgerTest {


    @Test
    void internBrukerBlirRejectedAvBorgerSjekk() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        KontekstHolder.setKontekst(kontekst);

        assertThrows(ManglerTilgangException.class, TilgangsstyringBorger::sjekkAtKallErFraBorger);
    }


    @Test
    void skalGiTilgangTilBorger() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        KontekstHolder.setKontekst(kontekst);

        assertDoesNotThrow(TilgangsstyringBorger::sjekkAtKallErFraBorger);
    }
}
