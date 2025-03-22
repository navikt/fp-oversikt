package no.nav.foreldrepenger.oversikt.aareg;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public record Permisjon(LocalDateInterval permisjonPeriode, Stillingsprosent permisjonsprosent, PermType permisjonstype) {
}
