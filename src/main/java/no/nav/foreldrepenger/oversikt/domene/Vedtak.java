package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.FpVedtak;

public record Vedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt,
                     @JsonProperty("perioder") List<Uttaksperiode> perioder,
                     @JsonProperty("dekningsgrad") Dekningsgrad dekningsgrad) {

    public FpVedtak tilDto() {
        var uttaksperioder = safeStream(perioder)
            .map(Uttaksperiode::tilDto)
            .toList();
        return new FpVedtak(uttaksperioder);
    }

    public boolean innvilget() {
        return perioder.stream().anyMatch(p -> p.resultat().innvilget());
    }
}
