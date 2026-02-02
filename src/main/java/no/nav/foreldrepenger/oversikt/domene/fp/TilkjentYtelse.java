package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus;

import java.time.LocalDate;
import java.util.List;

public record TilkjentYtelse(List<TilkjentYtelsePeriode> utbetalingsPerioder, List<FeriepengeAndel> feriepenger) {

    public record TilkjentYtelsePeriode(LocalDate fom, LocalDate tom, List<Andel> andeler) {
        public record Andel(AktivitetStatus aktivitetStatus, String arbeidsgiverIdent, String arbeidsgivernavn, Integer dagsats, boolean tilBruker,
                     Double utbetalingsgrad) {
        }
    }

    public record FeriepengeAndel(LocalDate opptjeningsår, Integer årsbeløp, String arbeidsgiverIdent, boolean tilBruker) {
    }

    public no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse tilDto() {
        var utbetalingsperioderMappet = utbetalingsPerioder == null ? List.<no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.TilkjentYtelsePeriode>of()
            : utbetalingsPerioder.stream().map(this::mapPeriode).toList();
        var feriepengerMappet = feriepenger == null ? List.<no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.FeriepengeAndel>of()
            : feriepenger.stream().map(this::mapFeriepengeAndel).toList();

        return new no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse(utbetalingsperioderMappet, feriepengerMappet);
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.TilkjentYtelsePeriode mapPeriode(TilkjentYtelsePeriode periode) {
        var andeler = periode.andeler() == null ? List.<no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.TilkjentYtelsePeriode.Andel>of()
            : periode.andeler().stream().map(this::mapAndel).toList();
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.TilkjentYtelsePeriode(periode.fom(), periode.tom(), andeler);
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.TilkjentYtelsePeriode.Andel mapAndel(TilkjentYtelsePeriode.Andel andel) {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.TilkjentYtelsePeriode.Andel(
            andel.aktivitetStatus(), andel.arbeidsgiverIdent(), andel.arbeidsgivernavn(), andel.dagsats(), andel.tilBruker(), andel.utbetalingsgrad());
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.FeriepengeAndel mapFeriepengeAndel(FeriepengeAndel andel) {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.FeriepengeAndel(andel.opptjeningsår(), andel.årsbeløp(), andel.arbeidsgiverIdent(), andel.tilBruker());
    }

}
