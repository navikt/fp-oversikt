package no.nav.foreldrepenger.oversikt.domene;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record AktørId(@JsonValue String value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AktørId {
        Objects.requireNonNull(value, "AktørId kan ikke være null");
    }

    @Override
    public String value() { // NOSONAR: Her overrider vi default getter fra record fordi den propagerer annoteringer fra field. Vi ønsker ikke @JsonValue på getter.
        return value;
    }

    @Override
    public String toString() {
        return "***";
    }

    public static AktørId dummy() {
        return new AktørId(UUID.randomUUID().toString());
    }
}
