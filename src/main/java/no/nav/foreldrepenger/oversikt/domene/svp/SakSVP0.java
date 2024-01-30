package no.nav.foreldrepenger.oversikt.domene.svp;

import static no.nav.foreldrepenger.oversikt.domene.NullUtil.nullSafe;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;

public record SakSVP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                      @JsonProperty("aktørId") AktørId aktørId,
                      @JsonProperty("avsluttet") boolean avsluttet,
                      @JsonProperty("familieHendelse") FamilieHendelse familieHendelse,
                      @JsonProperty("aksjonspunkt") Set<Aksjonspunkt> aksjonspunkt,
                      @JsonProperty("søknader") Set<SvpSøknad> søknader,
                      @JsonProperty("vedtak") Set<SvpVedtak> vedtak,
                      @JsonProperty("oppdatertTidspunkt") LocalDateTime oppdatertTidspunkt) implements Sak {

    @Override
    public Set<Aksjonspunkt> aksjonspunkt() {
        return nullSafe(aksjonspunkt);
    }

    @Override
    public Set<SvpSøknad> søknader() {
        return nullSafe(søknader);
    }

    @Override
    public Set<SvpVedtak> vedtak() {
        return nullSafe(vedtak);
    }

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
        return søknadUnderBehandling().map(s -> s.tilrettelegginger().isEmpty()).orElse(false) || familieHendelse == null;
    }

    @Override
    public YtelseType ytelse() {
        return YtelseType.SVANGERSKAPSPENGER;
    }

    @Override
    public no.nav.foreldrepenger.common.innsyn.svp.SvpSak tilSakDto(PersonOppslagSystem personOppslagSystem) {
        return DtoMapper.mapFra(this);
    }

    public Optional<SvpSøknad> søknadUnderBehandling() {
        return søknader().stream()
            .max(Comparator.comparing(SvpSøknad::mottattTidspunkt))
            .filter(sisteSøknad -> !sisteSøknad.status().behandlet());
    }
}
