package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public record Arbeidsavtale(LocalDateInterval arbeidsavtalePeriode, BigDecimal stillingsprosent) {
}
