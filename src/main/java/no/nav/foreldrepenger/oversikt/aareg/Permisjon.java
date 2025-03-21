package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public record Permisjon(LocalDateInterval permisjonPeriode, BigDecimal permisjonsprosent, String permisjonsÅrsak) {
}
