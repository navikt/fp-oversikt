package no.nav.foreldrepenger.oversikt.oppgave;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public record TilbakekrevingUttalelseOppgave(@NotNull Saksnummer saksnummer, @NotNull LocalDate opprettet, LocalDate frist) {
}
