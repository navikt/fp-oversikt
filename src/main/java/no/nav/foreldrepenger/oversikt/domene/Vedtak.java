package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.FpVedtak;

public record Vedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt,
                     @JsonProperty("uttak") Uttak uttak) {

    public FpVedtak tilDto() {
        if (uttak == null || uttak.perioder() == null) {
            return null;
        }

        var uttaksperioder = uttak.perioder().stream()
            .map(Uttaksperiode::tilDto)
            .toList();

        return new FpVedtak(uttaksperioder);
    }
}
