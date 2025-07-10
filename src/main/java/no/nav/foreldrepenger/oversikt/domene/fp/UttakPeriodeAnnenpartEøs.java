package no.nav.foreldrepenger.oversikt.domene.fp;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.common.innsyn.KontoType;

public record UttakPeriodeAnnenpartEøs(@NotNull LocalDate fom,
                                       @NotNull LocalDate tom,
                                       @NotNull KontoType kontoType,
                                       @NotNull BigDecimal trekkdager) {
}
