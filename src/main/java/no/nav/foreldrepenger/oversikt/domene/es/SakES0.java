package no.nav.foreldrepenger.oversikt.domene.es;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.EsSak;
import no.nav.foreldrepenger.common.innsyn.EsÅpenBehandling;
import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.BehandlingTilstandUtleder;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontroll;

public record SakES0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId,
                     @JsonProperty("avsluttet") boolean avsluttet,
                     @JsonProperty("familieHendelse") FamilieHendelse familieHendelse,
                     @JsonProperty("aksjonspunkt") Set<Aksjonspunkt> aksjonspunkt,
                     @JsonProperty("søknader") Set<EsSøknad> søknader,
                     @JsonProperty("vedtak") Set<EsVedtak> vedtak,
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
    public boolean erKomplettForVisning() {
        return familieHendelse == null;
    }

    @Override
    public YtelseType ytelse() {
        return YtelseType.ENGANGSSTØNAD;
    }

    @Override
    public no.nav.foreldrepenger.common.innsyn.EsSak tilSakDto(TilgangKontroll tilgangKontroll) {
        var familiehendelse = familieHendelse == null ? null : familieHendelse.tilDto();
        return new EsSak(saksnummer.tilDto(), familiehendelse, avsluttet, tilÅpenBehandling(), false, oppdatertTidspunkt());
    }

    private EsÅpenBehandling tilÅpenBehandling() {
        return søknadUnderBehandling()
            .map(s -> new EsÅpenBehandling(BehandlingTilstandUtleder.utled(aksjonspunkt())))
            .orElse(null);
    }

    private Optional<EsSøknad> søknadUnderBehandling() {
        return safeStream(søknader())
            .max(Comparator.comparing(EsSøknad::mottattTidspunkt))
            .filter(sisteSøknad -> !sisteSøknad.status().behandlet());
    }
}
