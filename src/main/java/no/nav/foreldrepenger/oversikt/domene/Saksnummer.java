package no.nav.foreldrepenger.oversikt.domene;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record Saksnummer(@JsonValue String value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Saksnummer {
        Objects.requireNonNull(value, "Saksnummer kan ikke være null");
    }

    @Override
    public String value() { // NOSONAR: Her overrider vi default getter fra record fordi den propagerer annoteringer fra field. Vi ønsker ikke @JsonValue på getter.
        return value;
    }

    public no.nav.foreldrepenger.common.innsyn.Saksnummer tilDto() {
        return new no.nav.foreldrepenger.common.innsyn.Saksnummer(value);
    }

}
