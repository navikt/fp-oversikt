package no.nav.foreldrepenger.oversikt.domene.fp;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;


public record FpVedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt,
                       @JsonProperty("perioder") List<Uttaksperiode> perioder,
                       @JsonProperty("dekningsgrad") Dekningsgrad dekningsgrad,
                       @JsonProperty("annenpartEøsUttaksperioder") List<UttakPeriodeAnnenpartEøs> annenpartEøsUttaksperioder,
                       @JsonProperty("beregningsgrunnlag") Beregningsgrunnlag beregningsgrunnlag,
                       @JsonProperty("tilkjentYtelse") TilkjentYtelse tilkjentYtelse) {

    public no.nav.foreldrepenger.kontrakter.fpoversikt.FpVedtak tilDto() {
        var bgDto = beregningsgrunnlag == null ? null : beregningsgrunnlag.tilDto();
        var tilkjentYtelseDto = tilkjentYtelse == null ? null : tilkjentYtelse.tilDto();

        return new no.nav.foreldrepenger.kontrakter.fpoversikt.FpVedtak(bgDto, tilkjentYtelseDto);
    }

    public boolean kanSøkeOmEndring() {
        // Vedtak som er innvilget uten trekkdager (annulleringer) gir ikke endringssøknad
        return Stream.ofNullable(perioder)
            .flatMap(Collection::stream)
            .anyMatch(p ->  p.resultat().innvilget() && p.harTrekkdager());
    }
}
