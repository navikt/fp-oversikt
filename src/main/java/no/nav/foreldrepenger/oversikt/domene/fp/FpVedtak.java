package no.nav.foreldrepenger.oversikt.domene.fp;

import static no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.compress;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak;
import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriode;

public record FpVedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt,
                       @JsonProperty("perioder") List<Uttaksperiode> perioder,
                       @JsonProperty("dekningsgrad") Dekningsgrad dekningsgrad,
                       @JsonProperty("annenpartEøsUttaksperioder") List<UttakPeriodeAnnenpartEøs> annenpartEøsUttaksperioder,
                       @JsonProperty("beregningsgrunnlag") Beregningsgrunnlag beregningsgrunnlag,
                       @JsonProperty("tilkjentYtelse") TilkjentYtelse tilkjentYtelse) {

    public no.nav.foreldrepenger.kontrakter.fpoversikt.FpVedtak tilDto(BrukerRolleSak brukerRolle) {
        var sortertUttaksperioder = Stream.ofNullable(perioder)
            .flatMap(Collection::stream)
            .map(p -> p.tilDto(brukerRolle))
            .sorted(Comparator.comparing(UttakPeriode::fom))
            .toList();
        var sortertUttaksperioderAnnenpartEøs = Stream.ofNullable(annenpartEøsUttaksperioder)
            .flatMap(Collection::stream)
            .map(UttakPeriodeAnnenpartEøs::tilDto)
            .sorted(Comparator.comparing(no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs::fom))
            .toList();
        var bgDto = beregningsgrunnlag == null ? null : beregningsgrunnlag.tilDto();
        var tilkjentYtelseDto = tilkjentYtelse == null ? null : tilkjentYtelse.tilDto();

        return new no.nav.foreldrepenger.kontrakter.fpoversikt.FpVedtak(compress(sortertUttaksperioder), sortertUttaksperioderAnnenpartEøs, bgDto, tilkjentYtelseDto);
    }

    public boolean innvilget() {
        return Stream.ofNullable(perioder)
            .flatMap(Collection::stream)
            .anyMatch(p -> p.resultat().innvilget());
    }
}
