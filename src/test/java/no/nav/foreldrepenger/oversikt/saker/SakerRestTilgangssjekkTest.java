package no.nav.foreldrepenger.oversikt.saker;

import no.nav.foreldrepenger.oversikt.arkiv.SafSelvbetjeningTjeneste;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import org.junit.jupiter.api.Test;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.umyndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SakerRestTilgangssjekkTest {

    private final static Saker saker = mock(Saker.class);


    @Test
    void myndig_innlogget_bruker_skal_ikke_gi_exception() {
        innloggetBorger();
        var myndigCase = new SakerRest(saker, mock(SafSelvbetjeningTjeneste.class), mock(InnloggetBruker.class), mock(TilgangKontrollTjeneste.class));
        assertThatCode(myndigCase::hent).doesNotThrowAnyException();
    }

    @Test
    void innlogget_saksbehandler_skal_ikke_få_tilgang_til_saker() {
        innloggetSaksbehandlerUtenDrift();
        var innloggetBruker = myndigInnloggetBruker();
        var tilgangskontroll = new TilgangKontrollTjeneste(null, innloggetBruker);
        var myndigCase = new SakerRest(saker, mock(SafSelvbetjeningTjeneste.class), innloggetBruker, tilgangskontroll);
        assertThatThrownBy(myndigCase::hent).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void umyndig_innlogget_bruker_skal_kaste_umyndigbruker_exception() {
        innloggetBorger();
        var innloggetBruker = umyndigInnloggetBruker();
        var tilgangskontroll = new TilgangKontrollTjeneste(null, innloggetBruker);
        var umyndigCase = new SakerRest(saker, mock(SafSelvbetjeningTjeneste.class), innloggetBruker, tilgangskontroll);
        assertThatThrownBy(umyndigCase::hent)
            .isInstanceOf(ManglerTilgangException.class)
            .extracting("feilKode")
            .isEqualTo(FeilKode.IKKE_TILGANG_UMYNDIG);
    }

}
