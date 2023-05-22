package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDateTime;

public record EsSøknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
}
