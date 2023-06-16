package no.nav.foreldrepenger.oversikt.domene.svp;

import static no.nav.foreldrepenger.oversikt.domene.NullUtil.nullSafe;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SvpVedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt,
                        @JsonProperty("arbeidsforhold") Set<ArbeidsforholdUttak> arbeidsforhold) {

    @Override
    public Set<ArbeidsforholdUttak> arbeidsforhold() {
        return nullSafe(arbeidsforhold);
    }
}
