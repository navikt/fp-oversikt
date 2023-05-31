package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class AdresseBeskyttelseOppslagTest {


    @Test
    void tomt_resultat_er_lik_ubeskyttet() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of());
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isFalse();
    }

    @Test
    void ugradert_er_lik_ubeskyttet() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.UGRADERT));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isFalse();
    }

    @Test
    void ugradert_og_fortrolig_gradering_fører_til_beskyttet_adresse() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.UGRADERT, AdresseBeskyttelse.Gradering.FORTROLIG));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isTrue();
    }
    @Test
    void strengt_fortrolig_fører_til_beskyttet_adresse() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.STRENGT_FORTROLIG));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isTrue();
    }

    @Test
    void fortrolig_fører_til_beskyttet_adresse() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.FORTROLIG));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isTrue();
    }

    @Test
    void strengt_fortrolig_utland_fører_til_beskyttet_adresse() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.STRENGT_FORTROLIG_UTLAND));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isTrue();
    }
}
