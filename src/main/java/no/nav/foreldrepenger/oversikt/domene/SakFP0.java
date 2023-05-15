package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.domene.BrukerRolle.*;
import static no.nav.foreldrepenger.oversikt.domene.SakStatus.avsluttet;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.FpÅpenBehandling;
import no.nav.foreldrepenger.common.innsyn.Person;
import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;


public record SakFP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId,
                     @JsonProperty("status") SakStatus status,
                     @JsonProperty("vedtakene") Set<FpVedtak> vedtakene,
                     @JsonProperty("annenPartAktørId") AktørId annenPartAktørId,
                     @JsonProperty("familieHendelse") FamilieHendelse familieHendelse,
                     @JsonProperty("aksjonspunkt") Set<Aksjonspunkt> aksjonspunkt,
                     @JsonProperty("søknader") Set<FpSøknad> søknader,
                     @JsonProperty("brukerRolle") BrukerRolle brukerRolle,
                     @JsonProperty("fødteBarn") Set<AktørId> fødteBarn) implements Sak {

    @Override
    public no.nav.foreldrepenger.common.innsyn.FpSak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag) {
        var gjeldendeVedtak = safeStream(vedtakene()).max(Comparator.comparing(FpVedtak::vedtakstidspunkt));
        var dekningsgrad = gjeldendeVedtak.map(vedtak -> vedtak.dekningsgrad().tilDto()).orElse(null);
        var fpVedtak = gjeldendeVedtak
            .map(FpVedtak::tilDto)
            .orElse(null);

        var annenPart = annenPartAktørId == null ? null : new Person(new Fødselsnummer(fødselsnummerOppslag.forAktørId(annenPartAktørId)), null);
        var kanSøkeOmEndring = gjeldendeVedtak.stream().anyMatch(FpVedtak::innvilget);
        var familiehendelse = familieHendelse == null ? null : familieHendelse.tilDto();
        var åpenBehandling = tilÅpenBehandling();
        var sisteSøknadMottattDato = safeStream(søknader)
            .max(Comparator.comparing(FpSøknad::mottattTidspunkt))
            .map(s -> s.mottattTidspunkt().toLocalDate())
            .orElse(null);
        var barna = safeStream(fødteBarn).map(b -> new Person(new Fødselsnummer(b.value()), null)).collect(Collectors.toSet());
        return new FpSak(saksnummer.tilDto(), avsluttet(status), sisteSøknadMottattDato, kanSøkeOmEndring, MOR.equals(brukerRolle()), false,
            false, false, false, null, annenPart, familiehendelse, fpVedtak, åpenBehandling, barna, dekningsgrad);
    }

    private FpÅpenBehandling tilÅpenBehandling() {
        var søknadUnderBehandling = søknadUnderBehandling();
        return søknadUnderBehandling.map(s -> {
            var perioder = s.perioder().stream().map(FpSøknadsperiode::tilDto).toList();
            return new FpÅpenBehandling(BehandlingTilstandUtleder.utled(aksjonspunkt()), perioder);
        }).orElse(null);
    }

    private Optional<FpSøknad> søknadUnderBehandling() {
        return safeStream(søknader())
            .max(Comparator.comparing(FpSøknad::mottattTidspunkt))
            .filter(sisteSøknad -> !sisteSøknad.status().behandlet());
    }
}
