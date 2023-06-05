package no.nav.foreldrepenger.oversikt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;

import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

public class KontestForTest {

    @BeforeEach
    public void initializeKontekst() {
        setKontestForBruker();
    }

    public static void setKontestForBruker() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        KontekstHolder.setKontekst(kontekst);
    }
}
