package no.nav.foreldrepenger.oversikt.oppgave;

public enum OppgaveStatus {
    OPPRETTET, //Opprettet, men ikke opprettet i ditt nav
    OPPRETTET_DITT_NAV, //Også opprettet i ditt nav
    AVSLUTTET, //Avsluttet, men ikke avsluttet i ditt nav
    AVSLUTTET_DITT_NAV //Også avsluttet i ditt nav
    ;

    public boolean aktiv() {
        return this == OPPRETTET || this == OPPRETTET_DITT_NAV;
    }
}
