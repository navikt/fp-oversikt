package no.nav.foreldrepenger.oversikt.drift;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.stub.TilgangKontrollStub;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

class ProsessTaskRestTjenesteTilgangTest {

    @Test
    void verifiserAtSamtligeEndepunktHarSjekk() {
        var prosessTaskRestTjeneste = new ProsessTaskRestTjeneste(null, TilgangKontrollStub.borger(true));

        assertThatThrownBy(() -> prosessTaskRestTjeneste.createProsessTask(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.restartProsessTask(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.retryAllProsessTask()).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.finnProsessTasks(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.searchProsessTasks(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.finnFeiletProsessTask(null)).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(() -> prosessTaskRestTjeneste.setFeiletProsessTaskFerdig(null)).isExactlyInstanceOf(ManglerTilgangException.class);
    }
}
