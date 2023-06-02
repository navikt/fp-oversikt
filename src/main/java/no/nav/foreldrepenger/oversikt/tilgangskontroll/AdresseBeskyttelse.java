package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import java.util.Set;

public record AdresseBeskyttelse(Set<Gradering> gradering) {

    public enum Gradering {
        GRADERT,
        UGRADERT,
    }

    public boolean harBeskyttetAdresse() {
        return !gradering.isEmpty() && !gradering.stream().allMatch(Gradering.UGRADERT::equals);
    }
}
