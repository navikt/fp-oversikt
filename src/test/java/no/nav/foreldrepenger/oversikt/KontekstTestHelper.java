package no.nav.foreldrepenger.oversikt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.vedtak.sikkerhet.kontekst.Groups;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

public class KontekstTestHelper {
    private static final Fødselsnummer FØDSELSNUMMER_DUMMY = new Fødselsnummer("99999999");

    public static void innloggetBorger() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        when(kontekst.getUid()).thenReturn(FØDSELSNUMMER_DUMMY.value());
        KontekstHolder.setKontekst(kontekst);
    }

    public static void innloggetSaksbehandlerUtenDrift() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        when(kontekst.getUid()).thenReturn(FØDSELSNUMMER_DUMMY.value());
        KontekstHolder.setKontekst(kontekst);
    }

    public static void innloggetSaksbehandlerMedDriftRolle() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        when(kontekst.harGruppe(Groups.DRIFT)).thenReturn(true);
        when(kontekst.getUid()).thenReturn(FØDSELSNUMMER_DUMMY.value());
        KontekstHolder.setKontekst(kontekst);
    }
}
