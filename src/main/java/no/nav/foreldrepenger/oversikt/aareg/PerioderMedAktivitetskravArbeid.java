package no.nav.foreldrepenger.oversikt.aareg;

import java.util.List;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public record PerioderMedAktivitetskravArbeid(Fødselsnummer morFødselsnummer, List<LocalDateInterval> aktivitetskravPerioder) {
}
