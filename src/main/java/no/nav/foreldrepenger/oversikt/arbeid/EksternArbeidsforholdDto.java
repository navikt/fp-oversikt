package no.nav.foreldrepenger.oversikt.arbeid;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;


public record EksternArbeidsforholdDto(@NotNull String arbeidsgiverId,
                                       @NotNull String arbeidsgiverIdType,
                                       @NotNull String arbeidsgiverNavn,
                                       @NotNull Stillingsprosent stillingsprosent,
                                       @NotNull LocalDate tom,
                                       LocalDate fom,
                                       @NotNull LocalDate from,
                                       Optional<LocalDate> to) {
}
