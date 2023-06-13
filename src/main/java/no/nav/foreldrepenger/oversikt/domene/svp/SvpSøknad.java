package no.nav.foreldrepenger.oversikt.domene.svp;

import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;

import java.time.LocalDateTime;

public record SvpSøknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
}
