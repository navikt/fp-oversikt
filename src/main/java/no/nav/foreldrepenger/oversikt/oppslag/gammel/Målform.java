package no.nav.foreldrepenger.oversikt.oppslag.gammel;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Målform {

    @JsonEnumDefaultValue
    NB,
    NN,
    EN,
    E;

    public static Målform standard() {
        return NB;
    }
}
