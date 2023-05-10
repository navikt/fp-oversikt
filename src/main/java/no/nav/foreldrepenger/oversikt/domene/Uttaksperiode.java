package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.common.innsyn.UttakPeriodeResultat;

public record Uttaksperiode(LocalDate fom, LocalDate tom, Resultat resultat) {

    public UttakPeriode tilDto() {
        //TODO resten av resultat
        var res = new UttakPeriodeResultat(resultat().innvilget(), false, false, null);
        return new UttakPeriode(fom, tom, null, res, null, null, null, null, null, null, false);
    }

    public record Resultat(Type type) {

        public boolean innvilget() {
            return Objects.equals(type, Type.INNVILGET);
        }

        public enum Type {
            INNVILGET,
            AVSLÅTT
        }
    }
}
