package no.nav.foreldrepenger.oversikt.domene;


import static no.nav.foreldrepenger.kontrakter.felles.validering.InputValideringRegex.BARE_TALL;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


public record Saksnummer(@JsonValue @NotNull @Pattern(regexp = BARE_TALL) String value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Saksnummer {
        Objects.requireNonNull(value, "Saksnummer kan ikke være null");
    }

    public static Saksnummer dummy() {
        return new Saksnummer(UUID.randomUUID().toString());
    }

    @Override
    public String value() { // NOSONAR: Her overrider vi default getter fra record fordi den propagerer annoteringer fra field. Vi ønsker ikke @JsonValue på getter.
        return value;
    }

    public no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer tilDto() {
        return new no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer(value);
    }

}
