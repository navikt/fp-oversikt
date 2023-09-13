package no.nav.foreldrepenger.oversikt.oppgave;


import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

    @Column(name = "opprettet_tid")
    private LocalDateTime opprettetTidspunkt;

    @Column(name = "opprettet_tid_dittnav")
    private LocalDateTime opprettetTidspunktDittNav;

    @Column(name = "avsluttet_tid")
    private LocalDateTime avsluttetTidspunkt;

    @Column(name = "avsluttet_tid_dittnav")
    private LocalDateTime avsluttetTidspunktDittNav;

    public OppgaveEntitet(Oppgave oppgave) {
        this.id = oppgave.id();
        this.saksnummer = oppgave.saksnummer().value();
        this.type = oppgave.type();
        this.opprettetTidspunkt = oppgave.status().opprettetTidspunkt();
        this.avsluttetTidspunkt = oppgave.status().avsluttetTidspunkt();
        this.opprettetTidspunktDittNav = oppgave.dittNavStatus().opprettetTidspunkt();
        this.avsluttetTidspunktDittNav = oppgave.dittNavStatus().avsluttetTidspunkt();
    }

    protected OppgaveEntitet() {

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

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public LocalDateTime getOpprettetTidspunktDittNav() {
        return opprettetTidspunktDittNav;
    }

    public LocalDateTime getAvsluttetTidspunkt() {
        return avsluttetTidspunkt;
    }

    public LocalDateTime getAvsluttetTidspunktDittNav() {
        return avsluttetTidspunktDittNav;
    }

    public void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public void setOpprettetTidspunktDittNav(LocalDateTime opprettetTidspunktDittNav) {
        this.opprettetTidspunktDittNav = opprettetTidspunktDittNav;
    }

    public void setAvsluttetTidspunkt(LocalDateTime avsluttetTidspunkt) {
        this.avsluttetTidspunkt = avsluttetTidspunkt;
    }

    public void setAvsluttetTidspunktDittNav(LocalDateTime avsluttetTidspunktDittNav) {
        this.avsluttetTidspunktDittNav = avsluttetTidspunktDittNav;
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
