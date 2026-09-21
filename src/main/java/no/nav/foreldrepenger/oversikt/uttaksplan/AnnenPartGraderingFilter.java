package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.util.List;

import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.Gradering;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.UttakDto;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

final class AnnenPartGraderingFilter {

    private AnnenPartGraderingFilter() {
    }

    static List<Planperiode> fjernArbeidsgivere(List<Planperiode> perioder) {
        return perioder.stream().map(periode -> filtrer(periode, false)).toList();
    }

    static List<Planperiode> fjernResultatOgArbeidsgivere(List<Planperiode> perioder) {
        return perioder.stream().map(periode -> filtrer(periode, true)).toList();
    }

    private static Planperiode filtrer(Planperiode periode, boolean fjernResultat) {
        var uttak = periode.uttak();
        var gradering = fjernArbeidsgiver(uttak.gradering());
        var resultat = fjernResultat ? null : uttak.resultat();
        if (gradering == uttak.gradering() && resultat == uttak.resultat()) {
            return periode;
        }
        return new Planperiode(periode.fom(), periode.tom(), new UttakDto(uttak.forelder(), uttak.kontoType(),
            uttak.utsettelseÅrsak(), uttak.overføringÅrsak(), gradering, uttak.morsAktivitet(), uttak.samtidigUttak(),
            uttak.flerbarnsdager(), resultat));
    }

    private static Gradering fjernArbeidsgiver(Gradering gradering) {
        if (gradering == null || gradering.aktivitet() == null) {
            return gradering;
        }
        return new Gradering(gradering.arbeidstidprosent(), null);
    }
}
