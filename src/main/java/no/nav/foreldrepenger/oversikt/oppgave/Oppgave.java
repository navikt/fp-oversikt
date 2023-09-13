package no.nav.foreldrepenger.oversikt.oppgave;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

record Oppgave(Saksnummer saksnummer, UUID id, OppgaveType type, Status status, StatusDittNav dittNavStatus) {

    Oppgave {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        Objects.requireNonNull(status);
    }

    @Override
    public StatusDittNav dittNavStatus() {
        return dittNavStatus == null ? new StatusDittNav(null, null) : dittNavStatus;
    }

    boolean aktiv() {
        return status.aktiv();
    }

    static class Status {
        private final LocalDateTime opprettetTidspunkt;
        private final LocalDateTime avsluttetTidspunkt;

        Status(LocalDateTime opprettetTidspunkt, LocalDateTime avsluttetTidspunkt) {
            this.opprettetTidspunkt = opprettetTidspunkt == null ? null : opprettetTidspunkt.truncatedTo(ChronoUnit.MILLIS);
            this.avsluttetTidspunkt = avsluttetTidspunkt == null ? null : avsluttetTidspunkt.truncatedTo(ChronoUnit.MILLIS);
        }

        boolean aktiv() {
            return avsluttetTidspunkt == null;
        }

        public LocalDateTime opprettetTidspunkt() {
            return opprettetTidspunkt;
        }

        public LocalDateTime avsluttetTidspunkt() {
            return avsluttetTidspunkt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Status status = (Status) o;
            return Objects.equals(opprettetTidspunkt, status.opprettetTidspunkt) && Objects.equals(avsluttetTidspunkt, status.avsluttetTidspunkt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(opprettetTidspunkt, avsluttetTidspunkt);
        }

        @Override
        public String toString() {
            return "Status{" + "opprettetTidspunkt=" + opprettetTidspunkt + ", avsluttetTidspunkt=" + avsluttetTidspunkt + '}';
        }
    }

    static class StatusDittNav extends Status {

        StatusDittNav(LocalDateTime opprettetTidspunkt, LocalDateTime avsluttetTidspunkt) {
            super(opprettetTidspunkt, avsluttetTidspunkt);
        }
    }
}
