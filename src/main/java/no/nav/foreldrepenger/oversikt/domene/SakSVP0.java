package no.nav.foreldrepenger.oversikt.domene;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.SvpSak;

public record SakSVP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                      @JsonProperty("aktørId") AktørId aktørId) implements Sak {
    @Override
    public no.nav.foreldrepenger.common.innsyn.SvpSak tilSakDto() {
        return new SvpSak(saksnummer.tilDto(), null, false, null);
    }
}
