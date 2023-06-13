package no.nav.foreldrepenger.oversikt.domene.es;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EsVedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt) {
}
