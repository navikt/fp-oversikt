package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

class AnnenPartRestAutoriseringTest {

    private AdresseBeskyttelseOppslag adresseBeskyttelseOppslag = mock(AdresseBeskyttelseOppslag.class);;

    @Test
    void sjekkAtEndepunktReturnereNullNårDetErBeskyttetAdresse() {
        innloggetBorger();
        when(adresseBeskyttelseOppslag.adresseBeskyttelse(any())).thenReturn(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
        var tilgangKontrollTjeneste = new TilgangKontrollTjeneste(null, null, adresseBeskyttelseOppslag);
        var annenPartRest = new AnnenPartRest(null, tilgangKontrollTjeneste, null, null);

        var request = new AnnenPartRest.AnnenPartVedtakRequest(new Fødselsnummer("12345678910"), null, null);

        assertThat(annenPartRest.hent(request)).isNull();
    }

    @Test
    void innloggetAnsattSkalIkkeHenteAnnenpartsVedtakEndepunktet() {
        innloggetSaksbehandlerUtenDrift();
        when(adresseBeskyttelseOppslag.adresseBeskyttelse(any())).thenReturn(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.UGRADERT)));
        var tilgangKontrollTjeneste = new TilgangKontrollTjeneste(null, null, adresseBeskyttelseOppslag);
        var annenPartRest = new AnnenPartRest(null, tilgangKontrollTjeneste, null, null);

        var request = new AnnenPartRest.AnnenPartVedtakRequest(new Fødselsnummer("12345678910"), null, null);

        assertThatThrownBy(() -> annenPartRest.hent(request)).isExactlyInstanceOf(ManglerTilgangException.class);
    }

}
