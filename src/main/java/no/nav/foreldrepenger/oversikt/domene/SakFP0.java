package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.util.Comparator;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.Person;
import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;


public record SakFP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId,
                     @JsonProperty("vedtakene") Set<Vedtak> vedtakene,
                     @JsonProperty("annenPartAktørId") AktørId annenPartAktørId) implements Sak {

    @Override
    public no.nav.foreldrepenger.common.innsyn.FpSak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag) {
        var gjeldendeVedtak = safeStream(vedtakene()).max(Comparator.comparing(Vedtak::vedtakstidspunkt));
        var dekningsgrad = gjeldendeVedtak.map(vedtak -> vedtak.dekningsgrad().tilDto()).orElse(null);
        var fpVedtak = gjeldendeVedtak
            .map(Vedtak::tilDto)
            .orElse(null);

        var annenPart = annenPartAktørId == null ? null : new Person(new Fødselsnummer(fødselsnummerOppslag.forAktørId(annenPartAktørId)), null);
        var kanSøkeOmEndring = gjeldendeVedtak.stream().anyMatch(Vedtak::innvilget);
        return new FpSak(saksnummer.tilDto(), false, null, kanSøkeOmEndring, false, false,
            false, false, false, null, annenPart, null,
            fpVedtak, null, null, dekningsgrad);
    }
}
