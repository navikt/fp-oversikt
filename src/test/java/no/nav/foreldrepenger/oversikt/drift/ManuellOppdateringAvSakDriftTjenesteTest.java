package no.nav.foreldrepenger.oversikt.drift;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

class ManuellOppdateringAvSakDriftTjenesteTest {

    @Test
    void verifiserSaksbehandlerUtenDriftRolleIkkeFårTilgang() {
        innloggetSaksbehandlerUtenDrift();
        var manuellOppdateringAvSakDriftTjeneste = new ManuellOppdateringAvSakDriftTjeneste(null);
        List<String> saksnummer = List.of();
        assertThatThrownBy(() -> manuellOppdateringAvSakDriftTjeneste.opprettHentSakTaskForSaksnummre(saksnummer))
            .isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void verifiserAtBorgerIkkeFårTilgang() {
        innloggetBorger();
        var manuellOppdateringAvSakDriftTjeneste = new ManuellOppdateringAvSakDriftTjeneste(null);
        List<String> saksnummer = List.of();
        assertThatThrownBy(() -> manuellOppdateringAvSakDriftTjeneste.opprettHentSakTaskForSaksnummre(saksnummer))
            .isExactlyInstanceOf(ManglerTilgangException.class);
    }
}
