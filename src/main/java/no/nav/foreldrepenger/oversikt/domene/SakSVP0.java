package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.domene.SakStatus.avsluttet;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.SvpSak;
import no.nav.foreldrepenger.common.innsyn.SvpÅpenBehandling;
import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;

public record SakSVP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                      @JsonProperty("aktørId") AktørId aktørId,
                      @JsonProperty("status") SakStatus status,
                      @JsonProperty("familieHendelse") FamilieHendelse familieHendelse,
                      @JsonProperty("aksjonspunkt") Set<Aksjonspunkt> aksjonspunkt,
                      @JsonProperty("søknader") Set<SvpSøknad> søknader,
                      @JsonProperty("vedtak") Set<SvpVedtak> vedtak,
                      @JsonProperty("oppdatertTidspunkt") LocalDateTime oppdatertTidspunkt) implements Sak {

    @Override
    public boolean harSøknad() {
        return søknader != null && !søknader.isEmpty();
    }

    @Override
    public boolean harVedtak() {
        return vedtak != null && !vedtak.isEmpty();
    }

    @Override
    public no.nav.foreldrepenger.common.innsyn.SvpSak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag) {
        var familiehendelse = familieHendelse == null ? null : familieHendelse.tilDto();
        return new SvpSak(saksnummer.tilDto(), familiehendelse, avsluttet(status), tilÅpenBehandling(), oppdatertTidspunkt());
    }

    private SvpÅpenBehandling tilÅpenBehandling() {
        return søknadUnderBehandling().map(s -> new SvpÅpenBehandling(BehandlingTilstandUtleder.utled(aksjonspunkt()))).orElse(null);
    }

    private Optional<SvpSøknad> søknadUnderBehandling() {
        return safeStream(søknader())
            .max(Comparator.comparing(SvpSøknad::mottattTidspunkt))
            .filter(sisteSøknad -> !sisteSøknad.status().behandlet());
    }
}
