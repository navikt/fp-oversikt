package no.nav.foreldrepenger.oversikt.domene;

import java.util.Set;

public final class NullUtil {

    private NullUtil() {
    }

     public static <T> Set<T> nullSafe(Set<T> objects) {
        return objects == null ? Set.of() : objects;
    }
}
