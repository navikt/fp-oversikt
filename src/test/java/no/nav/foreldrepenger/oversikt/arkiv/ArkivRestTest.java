package no.nav.foreldrepenger.oversikt.arkiv;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

class ArkivRestTest {

    private final ArkivTjeneste arkivTjeneste = mock(ArkivTjeneste.class);
    private final TilgangKontrollTjeneste tilgangkontroll = mock(TilgangKontrollTjeneste.class);
    private final InnloggetBruker innloggetBruker = mock(InnloggetBruker.class);
    private ArkivRest arkivRest;

    @BeforeEach
    void setUp() {
        arkivRest = new ArkivRest(arkivTjeneste, tilgangkontroll, innloggetBruker);
    }

    @Test
    void henterAlleJournalføringerHvisSaksnummerIkkeErOppgitt() {
        arkivRest.alleArkiverteDokumenterPåSak(null);
        verify(arkivTjeneste, times(1)).alle(any());
        verify(arkivTjeneste, times(0)).alle(any(), any());
    }

    @Test
    void hvisSaksnummerErOppgittHentesBareJournalposterPåSak() {
        arkivRest.alleArkiverteDokumenterPåSak(Saksnummer.dummy());
        verify(arkivTjeneste, times(0)).alle(any());
        verify(arkivTjeneste, times(1)).alle(any(), any());
        verify(tilgangkontroll, times(1)).sakKobletTilAktørGuard(any());
    }

}
