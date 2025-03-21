package no.nav.foreldrepenger.oversikt.aareg;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;

// Det er dette som FPsak får fra Abakus i dag. Før videre prosessering
public record GruppertArbeidsforholdPrArbeidsforholdId(ArbeidsforholdIdentifikator arbeidsforholdIdentifikator,
                                                       List<LocalDateInterval> ansettelsesPerioder,
                                                       List<Arbeidsavtale> arbeidsavtaler,
                                                       List<Permisjon> permisjoner) {

}
