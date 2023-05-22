package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDateTime;

public record SvpSøknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
}
