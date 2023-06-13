package no.nav.foreldrepenger.oversikt.domene.es;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;

public record EsSøknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
}
