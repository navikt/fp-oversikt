package no.nav.foreldrepenger.oversikt.domene.svp;

import static no.nav.foreldrepenger.oversikt.domene.NullUtil.nullSafe;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        return nullSafe(oppholdsperioder);
    }

    public enum ArbeidsforholdIkkeOppfyltÅrsak {
        ARBEIDSGIVER_KAN_TILRETTELEGGE,
        ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN,
        ANNET
    }
}
