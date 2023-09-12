package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.UUID;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

record Oppgave(Saksnummer saksnummer, UUID id, OppgaveType type, OppgaveStatus status) {
}
