package no.nav.foreldrepenger.oversikt.oppgave;

import java.time.LocalDate;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public record TilbakekrevingsInnslag(Saksnummer saksnummer, LocalDate opprettet) {
}
