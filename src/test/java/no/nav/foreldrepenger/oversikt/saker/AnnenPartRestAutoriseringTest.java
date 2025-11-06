package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenbrukerBeskyttetAdresse;
import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenpartUbeskyttetAdresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

class AnnenPartRestAutoriseringTest {

    @Test
    void sjekkAtEndepunktReturnereNullNårDetErBeskyttetAdresse() {
        innloggetBorger();
        var innloggetBruker = myndigInnloggetBruker();
        var tilgangKontrollTjeneste = new TilgangKontrollTjeneste(null, innloggetBruker);
        var annenPartRest = new AnnenPartRest(null, tilgangKontrollTjeneste, innloggetBruker, annenbrukerBeskyttetAdresse());

        var request = new AnnenPartRest.AnnenPartRequest(new Fødselsnummer("1"), null, null);

        assertThat(annenPartRest.hentVedtak(request)).isNull();
    }

    @Test
    void innloggetAnsattSkalIkkeHenteAnnenpartsVedtakEndepunktet() {
        innloggetSaksbehandlerUtenDrift();
        var tilgangKontrollTjeneste = new TilgangKontrollTjeneste(null, null);
        var annenPartRest = new AnnenPartRest(null, tilgangKontrollTjeneste, null, annenpartUbeskyttetAdresse());

        var request = new AnnenPartRest.AnnenPartRequest(new Fødselsnummer("1"), null, null);

        assertThatThrownBy(() -> annenPartRest.hentVedtak(request)).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void sjekkAtEndepunktReturnereNullNårUkjentFnr() {
        innloggetBorger();
        var innloggetBruker = myndigInnloggetBruker();
        var tilgangKontrollTjeneste = new TilgangKontrollTjeneste(null, innloggetBruker);
        var annenPartRest = new AnnenPartRest(null, tilgangKontrollTjeneste, innloggetBruker, DummyPersonOppslagSystemTest.annenbrukerBeskyttetAdresse(new BrukerIkkeFunnetIPdlException()));

        var request = new AnnenPartRest.AnnenPartRequest(new Fødselsnummer("1"), null, null);

        assertThat(annenPartRest.hentVedtak(request)).isNull();
    }


}
