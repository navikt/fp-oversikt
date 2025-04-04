package no.nav.foreldrepenger.oversikt.arbeid;

import java.time.LocalDate;
import java.util.Optional;


public record EksternArbeidsforholdDto(String arbeidsgiverId,
                                       String arbeidsgiverIdType,
                                       String arbeidsgiverNavn,
                                       Stillingsprosent stillingsprosent,
                                       LocalDate from,
                                       Optional<LocalDate> to) {
}
