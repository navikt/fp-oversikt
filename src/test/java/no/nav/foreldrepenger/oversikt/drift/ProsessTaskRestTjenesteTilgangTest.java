package no.nav.foreldrepenger.oversikt.drift;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.Groups;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

class ProsessTaskRestTjenesteTilgangTest {

    @Test
    void ansattMedDriftrolleSkalFåTilgang() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        when(kontekst.harGruppe(Groups.DRIFT)).thenReturn(true);
        KontekstHolder.setKontekst(kontekst);

        assertDoesNotThrow(ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift);
    }

    @Test
    void ansattMedSaksbehandlerRolleIKKESkalFåTilgang() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        when(kontekst.harGruppe(Groups.SAKSBEHANDLER)).thenReturn(true);
        KontekstHolder.setKontekst(kontekst);

        assertThrows(ManglerTilgangException.class, ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift);
    }

    @Test
    void eksternBrukerSkalIkkeFåTilgangTilDriftRessurser() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        KontekstHolder.setKontekst(kontekst);

        assertThrows(ManglerTilgangException.class, ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift);
    }

    @Test
    void verifiserAtSamtligeEndepunktHarSjekk() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        when(kontekst.harGruppe(Groups.SAKSBEHANDLER)).thenReturn(true);
        KontekstHolder.setKontekst(kontekst);

        var prosessTaskRestTjeneste = new ProsessTaskRestTjeneste(null);

        assertThrows(ManglerTilgangException.class, () -> prosessTaskRestTjeneste.createProsessTask(null));
        assertThrows(ManglerTilgangException.class, () -> prosessTaskRestTjeneste.restartProsessTask(null));
        assertThrows(ManglerTilgangException.class, () -> prosessTaskRestTjeneste.retryAllProsessTask());
        assertThrows(ManglerTilgangException.class, () -> prosessTaskRestTjeneste.finnProsessTasks(null));
        assertThrows(ManglerTilgangException.class, () -> prosessTaskRestTjeneste.searchProsessTasks(null));
        assertThrows(ManglerTilgangException.class, () -> prosessTaskRestTjeneste.finnFeiletProsessTask(null));
        assertThrows(ManglerTilgangException.class, () -> prosessTaskRestTjeneste.setFeiletProsessTaskFerdig(null));
    }
}
