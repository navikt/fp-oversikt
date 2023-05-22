package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDateTime;
import java.util.Set;

public record FpSøknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<FpSøknadsperiode> perioder) {

}
