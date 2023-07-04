package no.nav.foreldrepenger.oversikt.domene.svp;

import static no.nav.foreldrepenger.oversikt.domene.NullUtil.nullSafe;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SvpVedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt,
                        @JsonProperty("arbeidsforhold") Set<ArbeidsforholdUttak> arbeidsforhold,
                        @JsonProperty("avslagÅrsak") AvslagÅrsak avslagÅrsak) {

    @Override
    public Set<ArbeidsforholdUttak> arbeidsforhold() {
        return nullSafe(arbeidsforhold);
    }

    public enum AvslagÅrsak {
        ARBEIDSGIVER_KAN_TILRETTELEGGE,
        SØKER_ER_INNVILGET_SYKEPENGER,
        MANGLENDE_DOKUMENTASJON,
        ANNET,
    }
}
