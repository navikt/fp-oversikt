package no.nav.foreldrepenger.oversikt.domene.svp;

import static no.nav.foreldrepenger.oversikt.domene.NullUtil.nullSafe;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;

public record SvpSøknad(@JsonProperty("status") SøknadStatus status,
                        @JsonProperty("mottattTidspunkt") LocalDateTime mottattTidspunkt,
                        @JsonProperty("tilrettelegginger") Set<Tilrettelegging> tilrettelegginger) {

    public Set<Tilrettelegging> tilrettelegginger() {
        return nullSafe(tilrettelegginger);
    }
}
