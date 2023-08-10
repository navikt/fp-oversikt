package no.nav.foreldrepenger.oversikt.domene.svp;

import static no.nav.foreldrepenger.oversikt.domene.NullUtil.nullSafe;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record ArbeidsforholdUttak(@JsonProperty("aktivitet") Aktivitet aktivitet,
                                  @JsonProperty("behovFom") LocalDate behovFom,
                                  @JsonProperty("risikoFaktorer") String risikoFaktorer,
                                  @JsonProperty("tiltak") String tiltak,
                                  @JsonProperty("svpPerioder") Set<SvpPeriode> svpPerioder,
                                  @JsonProperty("oppholdsperioder") Set<OppholdPeriode> oppholdsperioder,
                                  @JsonProperty("ikkeOppfyltÅrsak") ArbeidsforholdIkkeOppfyltÅrsak ikkeOppfyltÅrsak
                                  ) {

    @Override
    public Set<SvpPeriode> svpPerioder() {
        return nullSafe(svpPerioder);
    }

    @Override
    public Set<OppholdPeriode> oppholdsperioder() {
        return fjernOverlapp(nullSafe(oppholdsperioder));
    }

    private static Set<OppholdPeriode> fjernOverlapp(Set<OppholdPeriode> perioder) {
        List<LocalDateSegment<OppholdPeriode>> segments = perioder.stream()
            .map(p -> new LocalDateSegment<>(p.fom(), p.tom(), p))
            .toList();
        return new LocalDateTimeline<>(segments, new OppholdPeriodeSegmentCombinator()).stream().map(LocalDateSegment::getValue).collect(Collectors.toSet());
    }

    public enum ArbeidsforholdIkkeOppfyltÅrsak {
        ARBEIDSGIVER_KAN_TILRETTELEGGE,
        ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN,
        ANNET
    }
}
