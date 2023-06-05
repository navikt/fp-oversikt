package no.nav.foreldrepenger.oversikt.saker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

class AnnenPartRestAutoriseringTest {

    private AdresseBeskyttelseOppslag adresseBeskyttelseOppslag = mock(AdresseBeskyttelseOppslag.class);;

    @BeforeAll
    public static void initializeKontekst() {
        setKontestForBruker();
    }

    @Test
    void sjekkAtEndepunktReturnereNullNårDetErBeskyttetAdresse() {
        when(adresseBeskyttelseOppslag.adresseBeskyttelse(any())).thenReturn(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
        var annenPartRest = new AnnenPartRest(null, null, null, adresseBeskyttelseOppslag);

        var request = new AnnenPartRest.AnnenPartVedtakRequest(new Fødselsnummer("12345678910"), null, null);

        assertThat(annenPartRest.hent(request)).isNull();
    }

    static void setKontestForBruker() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        KontekstHolder.setKontekst(kontekst);
    }
}
