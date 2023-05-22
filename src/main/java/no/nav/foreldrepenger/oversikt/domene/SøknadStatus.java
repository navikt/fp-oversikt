package no.nav.foreldrepenger.oversikt.domene;

public enum SøknadStatus {
    MOTTATT, BEHANDLET;

    public boolean behandlet() {
        return this.equals(BEHANDLET);
    }
}
