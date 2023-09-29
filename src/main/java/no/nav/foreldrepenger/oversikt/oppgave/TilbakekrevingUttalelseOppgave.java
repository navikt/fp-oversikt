package no.nav.foreldrepenger.oversikt.oppgave;

import java.time.LocalDate;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public record TilbakekrevingUttalelseOppgave(Saksnummer saksnummer, LocalDate opprettet, LocalDate frist) {
}
