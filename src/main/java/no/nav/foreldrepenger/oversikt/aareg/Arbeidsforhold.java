package no.nav.foreldrepenger.oversikt.aareg;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;


public record Arbeidsforhold(ArbeidsforholdIdentifikator arbeidsforholdIdentifikator,
                             LocalDateInterval ansettelsesPeriode,
                             LocalDateTimeline<Stillingsprosent> arbeidsavtaler,
                             LocalDateTimeline<List<Permisjon>> permisjoner) {


}
