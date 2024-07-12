package no.nav.foreldrepenger.oversikt.saker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.stub.TilgangKontrollStub;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

class AnnenPartRestAutoriseringTest {
    @Test
    void sjekkAtEndepunktReturnereNullNårDetErBeskyttetAdresse() {
        var annenPartRest = new AnnenPartRest(null, TilgangKontrollStub.beskyttetAdresse(), null, null);

        var request = new AnnenPartRest.AnnenPartRequest(new Fødselsnummer("1"), null, null);

        assertThat(annenPartRest.hent(request)).isNull();
    }

    @Test
    void innloggetAnsattSkalIkkeHenteAnnenpartsVedtakEndepunktet() {
        var annenPartRest = new AnnenPartRest(null, TilgangKontrollStub.saksbehandler(true), null, null);

        var request = new AnnenPartRest.AnnenPartRequest(new Fødselsnummer("1"), null, null);

        assertThatThrownBy(() -> annenPartRest.hent(request)).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void sjekkAtEndepunktReturnereNullNårUkjentFnr() {
        var annenPartRest = new AnnenPartRest(null, TilgangKontrollStub.ukjentFnr(), null, null);

        var request = new AnnenPartRest.AnnenPartRequest(new Fødselsnummer("1"), null, null);

        assertThat(annenPartRest.hent(request)).isNull();
    }
}
