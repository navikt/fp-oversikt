package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.domene.SakStatus.avsluttet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.FpÅpenBehandling;
import no.nav.foreldrepenger.common.innsyn.Person;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.common.util.StreamUtil;
import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;


public record SakFP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId,
                     @JsonProperty("status") SakStatus status,
                     @JsonProperty("vedtakene") Set<Vedtak> vedtakene,
                     @JsonProperty("annenPartAktørId") AktørId annenPartAktørId,
                     @JsonProperty("familieHendelse") FamilieHendelse familieHendelse,
                     @JsonProperty("aksjonspunkt") Set<Aksjonspunkt> aksjonspunkt,
                     @JsonProperty("egenskaper") Set<Egenskap> egenskaper) implements Sak {

    @Override
    public no.nav.foreldrepenger.common.innsyn.FpSak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag) {
        var gjeldendeVedtak = safeStream(vedtakene()).max(Comparator.comparing(Vedtak::vedtakstidspunkt));
        var dekningsgrad = gjeldendeVedtak.map(vedtak -> vedtak.dekningsgrad().tilDto()).orElse(null);
        var fpVedtak = gjeldendeVedtak
            .map(Vedtak::tilDto)
            .orElse(null);

        var annenPart = annenPartAktørId == null ? null : new Person(new Fødselsnummer(fødselsnummerOppslag.forAktørId(annenPartAktørId)), null);
        var kanSøkeOmEndring = gjeldendeVedtak.stream().anyMatch(Vedtak::innvilget);
        var familiehendelse = familieHendelse == null ? null : familieHendelse.tilDto();
        var åpenBehandling = tilÅpenBehandling();
        return new FpSak(saksnummer.tilDto(), avsluttet(status), null, kanSøkeOmEndring, false, false,
            false, false, false, null, annenPart, familiehendelse, fpVedtak, åpenBehandling, null, dekningsgrad);
    }

    private FpÅpenBehandling tilÅpenBehandling() {
        if (harSøknadUnderBehandling()) {
            var søknadsperioder = new ArrayList<UttakPeriode>(); //TODO søknadsperioder
            return new FpÅpenBehandling(BehandlingTilstandUtleder.utled(aksjonspunkt), søknadsperioder);
        }
        return null;
    }

    private boolean harSøknadUnderBehandling() {
        return StreamUtil.safeStream(egenskaper).anyMatch(e -> e == Egenskap.SØKNAD_UNDER_BEHANDLING);
    }
}
