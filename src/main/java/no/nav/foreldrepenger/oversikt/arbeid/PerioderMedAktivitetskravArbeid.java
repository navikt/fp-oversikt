package no.nav.foreldrepenger.oversikt.arbeid;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.util.List;

public record PerioderMedAktivitetskravArbeid(Fødselsnummer morFødselsnummer, List<LocalDateInterval> aktivitetskravPerioder) {
}
