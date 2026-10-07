package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.util.List;

import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Gradering;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakDto;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

final class AnnenPartGraderingFilter {

    private AnnenPartGraderingFilter() {
    }

    static List<Planperiode> fjernArbeidsgivere(List<Planperiode> perioder) {
        return perioder.stream().map(AnnenPartGraderingFilter::fjernArbeidsgiver).toList();
    }

    static List<Planperiode> fjernResultatOgArbeidsgivere(List<Planperiode> perioder) {
        return perioder.stream().map(p -> filtrer(p, true)).toList();
    }

    private static Planperiode fjernArbeidsgiver(Planperiode periode) {
        return filtrer(periode, false);
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
