package no.nav.foreldrepenger.oversikt.saker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.stub.TilgangKontrollStub;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

class SakerRestTilgangssjekkTest {

    private final static Saker saker = mock(Saker.class);


    @Test
    void myndig_innlogget_bruker_skal_ikke_gi_exception() {
        var myndigCase = new SakerRest(saker, TilgangKontrollStub.borger(true));
        assertThatCode(myndigCase::hent).doesNotThrowAnyException();
    }

    @Test
    void innlogget_saksbehandler_skal_ikke_få_tilgang_til_saker() {
        var myndigCase = new SakerRest(saker, TilgangKontrollStub.saksbehandler(false));
        assertThatThrownBy(myndigCase::hent).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void umyndig_innlogget_bruker_skal_kaste_umyndigbruker_exception() {
        var umyndigCase = new SakerRest(saker, TilgangKontrollStub.borger(false));
        assertThatThrownBy(umyndigCase::hent).isInstanceOf(ManglerTilgangException.class);
    }

}
