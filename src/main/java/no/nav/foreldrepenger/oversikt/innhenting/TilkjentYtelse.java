package no.nav.foreldrepenger.oversikt.innhenting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record TilkjentYtelse(List<TilkjentYtelsePeriode> utbetalingsPerioder, List<FeriepengeAndel> feriepenger) {

    record TilkjentYtelsePeriode(LocalDate fom, LocalDate tom, List<Andel> andeler) {
        record Andel(AktivitetStatus aktivitetStatus, String arbeidsgiverIdent, String arbeidsgivernavn, BigDecimal dagsats, boolean tilBruker,
                     BigDecimal utbetalingsgrad) {
        }
    }

    record FeriepengeAndel(LocalDate opptjeningsår, BigDecimal årsbeløp, String arbeidsgiverIdent, boolean tilBruker) {
    }
}
