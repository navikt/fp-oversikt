package no.nav.foreldrepenger.oversikt.domene;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.EsSak;
import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;

public record SakES0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId) implements Sak {
    @Override
    public no.nav.foreldrepenger.common.innsyn.EsSak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag) {
        return new EsSak(saksnummer.tilDto(), null, false, null, false);
    }
}
