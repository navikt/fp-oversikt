package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record FpSøknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<FpSøknadsperiode> perioder, Dekningsgrad dekningsgrad) {

}
