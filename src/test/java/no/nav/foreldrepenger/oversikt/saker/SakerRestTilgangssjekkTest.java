package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.umyndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class SakerRestTilgangssjekkTest {

    private final static Saker saker = mock(Saker.class);

    @Test
    void myndig_innlogget_bruker_skal_ikke_gi_exception() {
        var myndigCase = new SakerRest(saker, myndigInnloggetBruker());
        assertThatNoException().isThrownBy(myndigCase::hent);
    }

    @Test
    void umyndig_innlogget_bruker_skal_kaste_umyndigbruker_exception() {
        var umyndigCase = new SakerRest(saker, umyndigInnloggetBruker());
        assertThatThrownBy(umyndigCase::hent)
            .isInstanceOf(UmyndigBrukerException.class);
    }

}
