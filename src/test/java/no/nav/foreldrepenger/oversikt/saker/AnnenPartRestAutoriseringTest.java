package no.nav.foreldrepenger.oversikt.saker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;

class AnnenPartRestAutoriseringTest {

    AdresseBeskyttelseOppslag adresseBeskyttelseOppslag = mock(AdresseBeskyttelseOppslag.class);;

    @Test
    void sjekkAtEndepunktReturnereNullNårDetErBeskyttetAdresse() {
        when(adresseBeskyttelseOppslag.adresseBeskyttelse(any())).thenReturn(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.FORTROLIG)));
        var annenPartRest = new AnnenPartRest(null, null, null, adresseBeskyttelseOppslag);

        var request = new AnnenPartRest.AnnenPartVedtakRequest(new Fødselsnummer("12345678910"), null, null);

        assertThat(annenPartRest.hent(request)).isNull();
    }
}
