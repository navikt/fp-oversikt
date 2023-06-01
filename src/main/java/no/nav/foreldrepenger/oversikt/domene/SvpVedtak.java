package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SvpVedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt) {
}
