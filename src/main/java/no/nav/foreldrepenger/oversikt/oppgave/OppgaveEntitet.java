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
import jakarta.validation.constraints.NotNull;

@Entity(name = "oppgave")
@Table(name = "oppgave")
class OppgaveEntitet {

    @Id
    private UUID id;

    // TODO (anyone): migrer til not null i DB og sett nullable = false her. Se fp-inntektsmelding / migrering for inspirasjon
    @Column(name = "saksnummer", updatable = false)
    private String saksnummer;

    @NotNull
    @Column(name = "type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private OppgaveType type;

    @NotNull
    @Column(name = "opprettet_tid", nullable = false)
    private LocalDateTime opprettetTidspunkt;

    @Column(name = "avsluttet_tid")
    private LocalDateTime avsluttetTidspunkt;

    public OppgaveEntitet(Oppgave oppgave) {
        this.id = oppgave.id();
        this.saksnummer = oppgave.saksnummer().value();
        this.type = oppgave.type();
        this.opprettetTidspunkt = oppgave.status().opprettetTidspunkt();
        this.avsluttetTidspunkt = oppgave.status().avsluttetTidspunkt();
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

    public LocalDateTime getAvsluttetTidspunkt() {
        return avsluttetTidspunkt;
    }

    public void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public void setAvsluttetTidspunkt(LocalDateTime avsluttetTidspunkt) {
        this.avsluttetTidspunkt = avsluttetTidspunkt;
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
