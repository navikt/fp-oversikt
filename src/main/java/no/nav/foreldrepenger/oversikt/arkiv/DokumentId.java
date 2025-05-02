package no.nav.foreldrepenger.oversikt.arkiv;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record DokumentId(@JsonValue @NotNull @Digits(integer = 18, fraction = 0) String verdi) {
}
