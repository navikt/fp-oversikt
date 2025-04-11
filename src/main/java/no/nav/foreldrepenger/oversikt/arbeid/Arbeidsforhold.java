package no.nav.foreldrepenger.oversikt.arbeid;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.util.List;


public record Arbeidsforhold(ArbeidsforholdIdentifikator arbeidsforholdIdentifikator,
                             LocalDateInterval ansettelsesPeriode,
                             LocalDateTimeline<Stillingsprosent> arbeidsavtaler,
                             LocalDateTimeline<List<Permisjon>> permisjoner) {


}
