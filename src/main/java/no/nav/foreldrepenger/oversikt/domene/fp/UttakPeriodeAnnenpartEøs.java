package no.nav.foreldrepenger.oversikt.domene.fp;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record UttakPeriodeAnnenpartEøs(@NotNull LocalDate fom,
                                       @NotNull LocalDate tom,
                                       @NotNull Konto kontoType,
                                       @NotNull BigDecimal trekkdager) {

    public no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs tilDto() {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs(fom, tom, kontoType.tilDto(), kontoType.tilDto(), new no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs.Trekkdager(trekkdager));
    }
}
