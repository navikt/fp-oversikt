package no.nav.foreldrepenger.oversikt.aareg;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public record Arbeidsavtale(LocalDateInterval arbeidsavtalePeriode, Stillingsprosent stillingsprosent) {
}
