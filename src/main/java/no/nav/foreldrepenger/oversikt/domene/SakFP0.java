package no.nav.foreldrepenger.oversikt.domene;

import java.util.Comparator;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.FpSak;


public record SakFP0(@JsonProperty("saksnummer") Saksnummer saksnummer,
                     @JsonProperty("aktørId") AktørId aktørId,

                     @JsonProperty("vedtakene") Set<Vedtak> vedtakene) implements Sak {

    @Override
    public no.nav.foreldrepenger.common.innsyn.FpSak tilSakDto() {
        var gjeldendeVedtak = vedtakene.stream().max(Comparator.comparing(Vedtak::vedtakstidspunkt));
        var dekningsgrad = gjeldendeVedtak.map(vedtak -> vedtak.uttak().dekningsgrad().tilDto()).orElse(null);
        var fpVedtak = gjeldendeVedtak
            .map(Vedtak::tilDto)
            .orElse(null);

        return new FpSak(saksnummer.tilDto(), false, null, false, false, false,
            false, false, false, null, null, null,
            fpVedtak, null, null, dekningsgrad);
    }
}
