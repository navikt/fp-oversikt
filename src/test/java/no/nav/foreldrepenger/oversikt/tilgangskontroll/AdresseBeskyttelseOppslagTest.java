package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.pdl.AdressebeskyttelseGradering;

class AdresseBeskyttelseOppslagTest {


    @Test
    void null_resultat_er_lik_ubeskyttet() {
        var annenPartAutorisering = new AdresseBeskyttelse(null);
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isFalse();
    }

    @Test
    void tomt_resultat_er_lik_ubeskyttet() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of());
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isFalse();
    }

    @Test
    void ugradert_er_lik_ubeskyttet() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdressebeskyttelseGradering.UGRADERT));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isFalse();
    }

    @Test
    void ugradert_og_fortrolig_gradering_fører_til_beskyttet_adresse() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdressebeskyttelseGradering.UGRADERT, AdressebeskyttelseGradering.FORTROLIG));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isTrue();
    }
    @Test
    void strengt_fortrolig_fører_til_beskyttet_adresse() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdressebeskyttelseGradering.STRENGT_FORTROLIG));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isTrue();
    }

    @Test
    void fortrolig_fører_til_beskyttet_adresse() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdressebeskyttelseGradering.FORTROLIG));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isTrue();
    }

    @Test
    void strengt_fortrolig_utland_fører_til_beskyttet_adresse() {
        var annenPartAutorisering = new AdresseBeskyttelse(Set.of(AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND));
        assertThat(annenPartAutorisering.harBeskyttetAdresse()).isTrue();
    }
}
