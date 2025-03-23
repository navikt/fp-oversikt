package no.nav.foreldrepenger.oversikt.aareg;

import java.time.LocalDate;
import java.util.Optional;


public record EksternArbeidsforhold(String arbeidsgiverId,
                                    String arbeidsgiverIdType,
                                    LocalDate from,
                                    Optional<LocalDate> to,
                                    Stillingsprosent stillingsprosent,
                                    String arbeidsgiverNavn) {

}
