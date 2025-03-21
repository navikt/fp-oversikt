package no.nav.foreldrepenger.oversikt.aareg;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;


public record Arbeidsforhold(ArbeidsforholdIdentifikator arbeidsforholdIdentifikator,
                             LocalDateInterval ansettelsesPeriode,
                             List<Arbeidsavtale> arbeidsavtaler,
                             List<Permisjon> permisjoner) {


}
