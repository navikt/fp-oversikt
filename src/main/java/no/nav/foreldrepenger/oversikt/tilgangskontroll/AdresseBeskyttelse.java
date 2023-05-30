package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import java.util.Set;

import no.nav.pdl.AdressebeskyttelseGradering;

public record AdresseBeskyttelse(Set<AdressebeskyttelseGradering> gradering) {

    public boolean harBeskyttetAdresse() {
        if (gradering == null || gradering.isEmpty() || gradering.stream().allMatch(AdressebeskyttelseGradering.UGRADERT::equals)) {
            return false;
        }
        return true;
    }

}
