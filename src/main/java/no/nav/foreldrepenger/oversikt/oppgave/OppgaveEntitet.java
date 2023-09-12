package no.nav.foreldrepenger.oversikt.oppgave;


import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity(name = "oppgave")
@Table(name = "oppgave")
class OppgaveEntitet {

    @Id
    private UUID id;

    @Column(name = "saksnummer", nullable = false, updatable = false)
    private String saksnummer;

    @Column(name = "type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private OppgaveType type;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OppgaveStatus status;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt;

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    public OppgaveEntitet(UUID id, String saksnummer, OppgaveType type, OppgaveStatus status) {
        this.id = id;
        this.saksnummer = saksnummer;
        this.type = type;
        this.status = status;
    }

    protected OppgaveEntitet() {

    }

    @PrePersist
    protected void onCreate() {
        this.opprettetTidspunkt = opprettetTidspunkt != null ? opprettetTidspunkt : LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        endretTidspunkt = LocalDateTime.now();
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public UUID getId() {
        return id;
    }

    public OppgaveType getType() {
        return type;
    }

    public OppgaveStatus getStatus() {
        return status;
    }

    public void endreStatus(OppgaveStatus nyStatus) {
        status = nyStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OppgaveEntitet that = (OppgaveEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
