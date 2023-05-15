package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.oversikt.domene.SakStatus.avsluttet;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.SvpSak;
import no.nav.foreldrepenger.common.innsyn.SvpÅpenBehandling;
import no.nav.foreldrepenger.common.util.StreamUtil;
import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;

public record SakSVP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                      @JsonProperty("aktørId") AktørId aktørId,
                      @JsonProperty("status") SakStatus status,
                      @JsonProperty("familieHendelse") FamilieHendelse familieHendelse,
                      @JsonProperty("aksjonspunkt") Set<Aksjonspunkt> aksjonspunkt,
                      @JsonProperty("egenskaper") Set<Egenskap> egenskaper) implements Sak {
    @Override
    public no.nav.foreldrepenger.common.innsyn.SvpSak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag) {
        var familiehendelse = familieHendelse == null ? null : familieHendelse.tilDto();
        return new SvpSak(saksnummer.tilDto(), familiehendelse, avsluttet(status), tilÅpenBehandling());
    }

    private SvpÅpenBehandling tilÅpenBehandling() {
        return harSøknadUnderBehandling() ? new SvpÅpenBehandling(BehandlingTilstandUtleder.utled(aksjonspunkt)) : null;
    }

    private boolean harSøknadUnderBehandling() {
        return StreamUtil.safeStream(egenskaper).anyMatch(e -> e == Egenskap.SØKNAD_UNDER_BEHANDLING);
    }
}
