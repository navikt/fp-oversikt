package no.nav.foreldrepenger.oversikt.oppgave;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

record Oppgave(Saksnummer saksnummer, UUID id, OppgaveType type, Status status) {

    Oppgave {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        Objects.requireNonNull(status);
    }

    boolean aktiv() {
        return status.aktiv();
    }

    record Status(LocalDateTime opprettetTidspunkt, LocalDateTime avsluttetTidspunkt) {

        Status(LocalDateTime opprettetTidspunkt, LocalDateTime avsluttetTidspunkt) {
            this.opprettetTidspunkt = opprettetTidspunkt == null ? null : opprettetTidspunkt.truncatedTo(ChronoUnit.MILLIS);
            this.avsluttetTidspunkt = avsluttetTidspunkt == null ? null : avsluttetTidspunkt.truncatedTo(ChronoUnit.MILLIS);
        }

        boolean aktiv() {
            return avsluttetTidspunkt == null;
        }
    }
}
