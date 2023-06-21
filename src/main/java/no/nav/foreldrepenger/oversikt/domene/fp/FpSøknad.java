package no.nav.foreldrepenger.oversikt.domene.fp;

import static no.nav.foreldrepenger.oversikt.domene.NullUtil.nullSafe;

import java.time.LocalDateTime;
import java.util.Set;

import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;

public record FpSøknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<FpSøknadsperiode> perioder, Dekningsgrad dekningsgrad) {

    @Override
    public Set<FpSøknadsperiode> perioder() {
        return nullSafe(perioder);
    }
}
