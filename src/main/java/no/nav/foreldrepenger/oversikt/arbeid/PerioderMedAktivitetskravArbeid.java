package no.nav.foreldrepenger.oversikt.arbeid;

import java.util.List;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.fpsak.tidsserie.LocalDateSegment;

public record PerioderMedAktivitetskravArbeid(Fødselsnummer morFødselsnummer, List<LocalDateSegment<PeriodeMedAktivitetskravType>> aktivitetskravPerioder) {
}
