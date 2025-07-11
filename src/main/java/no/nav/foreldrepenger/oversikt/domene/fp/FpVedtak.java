package no.nav.foreldrepenger.oversikt.domene.fp;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.compress;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.BrukerRolleSak;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;

public record FpVedtak(@JsonProperty("vedtakstidspunkt") LocalDateTime vedtakstidspunkt,
                       @JsonProperty("perioder") List<Uttaksperiode> perioder,
                       @JsonProperty("dekningsgrad") Dekningsgrad dekningsgrad) {

    public no.nav.foreldrepenger.common.innsyn.FpVedtak tilDto(BrukerRolleSak brukerRolle) {
        var sortert = safeStream(perioder).map(p -> p.tilDto(brukerRolle)).sorted(Comparator.comparing(UttakPeriode::fom)).toList();
        var compressed = compress(sortert);
        return new no.nav.foreldrepenger.common.innsyn.FpVedtak(compressed);
    }

    public boolean innvilget() {
        return safeStream(perioder).anyMatch(p -> p.resultat().innvilget());
    }
}
