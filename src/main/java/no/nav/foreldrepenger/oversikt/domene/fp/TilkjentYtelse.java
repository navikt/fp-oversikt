package no.nav.foreldrepenger.oversikt.domene.fp;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TilkjentYtelse(List<TilkjentYtelsePeriode> utbetalingsperioder, List<FeriepengeAndel> feriepenger) {

    public record TilkjentYtelsePeriode(LocalDate fom, LocalDate tom, List<Andel> andeler) {
        public record Andel(AktivitetStatus aktivitetStatus, String arbeidsgiverIdent, String arbeidsgivernavn, BigDecimal dagsats, boolean tilBruker,
                            BigDecimal utbetalingsgrad) {
        }
    }

    public record FeriepengeAndel(LocalDate opptjeningsår, BigDecimal årsbeløp, String arbeidsgiverIdent, boolean tilBruker) {
    }

    public no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse tilDto() {
        List<no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.TilkjentYtelsePeriode> utbetalingsperioderMappet = utbetalingsperioder == null
            ? List.of()
            : utbetalingsperioder.stream().map(this::mapPeriode).toList();
        var feriepengerMappet = feriepenger == null
            ? List.<no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.FeriepengeAndel>of()
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
            andel.aktivitetStatus().tilDto(), andel.arbeidsgiverIdent(), andel.arbeidsgivernavn(), andel.dagsats(), andel.tilBruker(), andel.utbetalingsgrad());
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.FeriepengeAndel mapFeriepengeAndel(FeriepengeAndel andel) {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.TilkjentYtelse.FeriepengeAndel(andel.opptjeningsår(), andel.årsbeløp(), andel.arbeidsgiverIdent(), andel.tilBruker());
    }

}
