package no.nav.foreldrepenger.oversikt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.vedtak.sikkerhet.kontekst.Groups;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

public class KontekstTestHelper {

    public static void innloggetBorger() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        KontekstHolder.setKontekst(kontekst);
    }

    public static void innloggetSaksbehandler() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        KontekstHolder.setKontekst(kontekst);
    }

    public static void innloggetSaksbehandlerMedDriftRolle() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        when(kontekst.harGruppe(Groups.DRIFT)).thenReturn(true);
        KontekstHolder.setKontekst(kontekst);
    }
}
