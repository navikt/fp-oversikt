package no.nav.foreldrepenger.oversikt.aareg;

import java.util.List;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public record PerioderMedAktivitetskravArbeid(AktørId morAktørId, List<LocalDateInterval> aktivitetskravPerioder) {
}
