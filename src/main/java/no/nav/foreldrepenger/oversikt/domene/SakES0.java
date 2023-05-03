package no.nav.foreldrepenger.oversikt.domene;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.EsSak;

public record SakES0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId) implements Sak {
    @Override
    public no.nav.foreldrepenger.common.innsyn.EsSak tilSakDto() {
        return new EsSak(saksnummer.tilDto(), null, false, null, false);
    }
}
