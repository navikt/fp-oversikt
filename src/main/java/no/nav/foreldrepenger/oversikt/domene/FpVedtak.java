package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FpVedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt,
                       @JsonProperty("perioder") List<Uttaksperiode> perioder,
                       @JsonProperty("dekningsgrad") Dekningsgrad dekningsgrad) {

    public no.nav.foreldrepenger.common.innsyn.FpVedtak tilDto() {
        var uttaksperioder = safeStream(perioder)
            .map(Uttaksperiode::tilDto)
            .toList();
        return new no.nav.foreldrepenger.common.innsyn.FpVedtak(uttaksperioder);
    }

    public boolean innvilget() {
        return safeStream(perioder).anyMatch(p -> p.resultat().innvilget());
    }
}
