package no.nav.foreldrepenger.oversikt.server.sikkerhet;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerMedDriftRolle;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

class ForvaltningAuthorizationFilterTest {

    @Test
    void ansattMedDriftrolleSkalFåTilgang() {
        innloggetSaksbehandlerMedDriftRolle();
        var forvaltningFilter = new ForvaltningAuthorizationFilter();
        Assertions.assertDoesNotThrow(() -> forvaltningFilter.filter(null));
    }

    @Test
    void ansattMedSaksbehandlerRolleIKKESkalFåTilgang() {
        innloggetSaksbehandlerUtenDrift();
        var forvaltningFilter = new ForvaltningAuthorizationFilter();
        assertThatThrownBy(() -> forvaltningFilter.filter(null)).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void eksternBrukerSkalIkkeFåTilgangTilDriftRessurser() {
        innloggetBorger();
        var forvaltningFilter = new ForvaltningAuthorizationFilter();
        assertThatThrownBy(() -> forvaltningFilter.filter(null)).isExactlyInstanceOf(ManglerTilgangException.class);
    }
}
