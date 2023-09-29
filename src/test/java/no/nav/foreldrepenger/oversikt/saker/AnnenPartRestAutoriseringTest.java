package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

class AnnenPartRestAutoriseringTest {

    @Test
    void sjekkAtEndepunktReturnereNullNårDetErBeskyttetAdresse() {
        innloggetBorger();
        var tilgangKontrollTjeneste = new TilgangKontrollTjeneste(null, null, fnr -> new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
        var annenPartRest = new AnnenPartRest(null, tilgangKontrollTjeneste, null, null);

        var request = new AnnenPartRest.AnnenPartVedtakRequest(new Fødselsnummer("12345678910"), null, null);

        assertThat(annenPartRest.hent(request)).isNull();
    }

    @Test
    void innloggetAnsattSkalIkkeHenteAnnenpartsVedtakEndepunktet() {
        innloggetSaksbehandlerUtenDrift();
        var tilgangKontrollTjeneste = new TilgangKontrollTjeneste(null, null, fnr -> new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
        var annenPartRest = new AnnenPartRest(null, tilgangKontrollTjeneste, null, null);

        var request = new AnnenPartRest.AnnenPartVedtakRequest(new Fødselsnummer("12345678910"), null, null);

        assertThatThrownBy(() -> annenPartRest.hent(request)).isExactlyInstanceOf(ManglerTilgangException.class);
    }

}
