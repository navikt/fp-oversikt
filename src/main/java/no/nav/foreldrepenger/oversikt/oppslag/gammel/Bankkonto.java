package no.nav.foreldrepenger.oversikt.oppslag.gammel;


public record Bankkonto(String kontonummer, String banknavn) {

    public static final Bankkonto UKJENT = new Bankkonto("", "");

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [kontonummer='********', banknavn=" + banknavn + "]";
    }
}
