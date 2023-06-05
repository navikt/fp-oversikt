package no.nav.foreldrepenger.oversikt.drift;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandler;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerMedDriftRolle;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

class ProsessTaskRestTjenesteTilgangTest {

    @Test
    void ansattMedDriftrolleSkalFåTilgang() {
        innloggetSaksbehandlerMedDriftRolle();
        assertThatCode(ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift).doesNotThrowAnyException();
    }

    @Test
    void ansattMedSaksbehandlerRolleIKKESkalFåTilgang() {
        innloggetSaksbehandler();
        assertThatThrownBy(ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void eksternBrukerSkalIkkeFåTilgangTilDriftRessurser() {
        innloggetBorger();
        assertThatThrownBy(ProsessTaskRestTjeneste::sjekkAtSaksbehandlerHarRollenDrift).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void verifiserAtSamtligeEndepunktHarSjekk() {
        innloggetSaksbehandler();
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
