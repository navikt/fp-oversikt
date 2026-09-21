package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

public record AnnenPartUttaksplan(LocalDateTime mottattTidspunkt, List<Planperiode> perioder) {
}
