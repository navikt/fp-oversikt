package no.nav.foreldrepenger.oversikt.drift;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        assertThatCode(ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift).doesNotThrowAnyException();
    }

    @Test
    void ansattMedSaksbehandlerRolleIKKESkalFåTilgang() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        when(kontekst.harGruppe(Groups.SAKSBEHANDLER)).thenReturn(true);
        KontekstHolder.setKontekst(kontekst);

        assertThatThrownBy(ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void eksternBrukerSkalIkkeFåTilgangTilDriftRessurser() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        KontekstHolder.setKontekst(kontekst);

        assertThatThrownBy(ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void verifiserAtSamtligeEndepunktHarSjekk() {
        var kontekst = mock(RequestKontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        when(kontekst.harGruppe(Groups.SAKSBEHANDLER)).thenReturn(true);
        KontekstHolder.setKontekst(kontekst);

        var prosessTaskRestTjeneste = new ProsessTaskRestTjeneste(null);

        assertThatThrownBy(() -> prosessTaskRestTjeneste.createProsessTask(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.restartProsessTask(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.retryAllProsessTask()).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.finnProsessTasks(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.searchProsessTasks(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.finnFeiletProsessTask(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.setFeiletProsessTaskFerdig(null)).isExactlyInstanceOf(ManglerTilgangException.class);
    }
}
