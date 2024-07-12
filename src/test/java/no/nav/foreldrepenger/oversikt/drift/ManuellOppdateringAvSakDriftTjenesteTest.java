package no.nav.foreldrepenger.oversikt.drift;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.stub.TilgangKontrollStub;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

class ManuellOppdateringAvSakDriftTjenesteTest {

    @Test
    void verifiserSaksbehandlerUtenDriftRolleIkkeFårTilgang() {
        var manuellOppdateringAvSakDriftTjeneste = new ManuellOppdateringAvSakDriftTjeneste(null, TilgangKontrollStub.saksbehandler(false));
        assertThatThrownBy(() -> manuellOppdateringAvSakDriftTjeneste.opprettHentSakTaskForSaksnummre(List.of()))
            .isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void verifiserAtBorgerIkkeFårTilgang() {
        var manuellOppdateringAvSakDriftTjeneste = new ManuellOppdateringAvSakDriftTjeneste(null, TilgangKontrollStub.borger(true));
        assertThatThrownBy(() -> manuellOppdateringAvSakDriftTjeneste.opprettHentSakTaskForSaksnummre(List.of()))
            .isExactlyInstanceOf(ManglerTilgangException.class);
    }
}
