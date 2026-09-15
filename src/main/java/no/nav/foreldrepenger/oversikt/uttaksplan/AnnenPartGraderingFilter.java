package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.util.List;

import no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.Gradering;
import no.nav.foreldrepenger.oversikt.uttaksplan.FellesUttaksplanDto.UttakDto;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

final class AnnenPartGraderingFilter {

    private AnnenPartGraderingFilter() {
    }

    static List<Planperiode> fjernArbeidsgivere(List<Planperiode> perioder) {
        return perioder.stream().map(AnnenPartGraderingFilter::fjernArbeidsgiver).toList();
    }

    private static Planperiode fjernArbeidsgiver(Planperiode periode) {
        var uttak = periode.uttak();
        var gradering = fjernArbeidsgiver(uttak.gradering());
        if (gradering == uttak.gradering()) {
            return periode;
        }
        return new Planperiode(periode.fom(), periode.tom(), new UttakDto(uttak.forelder(), uttak.kontoType(),
            uttak.utsettelseÅrsak(), uttak.overføringÅrsak(), gradering, uttak.morsAktivitet(), uttak.samtidigUttak(),
            uttak.flerbarnsdager(), uttak.resultat()));
    }

    private static Gradering fjernArbeidsgiver(Gradering gradering) {
        if (gradering == null || gradering.aktivitet() == null) {
            return gradering;
        }
        return new Gradering(gradering.arbeidstidprosent(), null);
    }
}
