package no.nav.foreldrepenger.oversikt.arbeid;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Optional;


public record EksternArbeidsforholdDto(@NotNull String arbeidsgiverId,
                                       @NotNull String arbeidsgiverIdType,
                                       @NotNull String arbeidsgiverNavn,
                                       @NotNull Stillingsprosent stillingsprosent,
                                       @NotNull LocalDate from,
                                       Optional<LocalDate> to) {
}
