package no.nav.foreldrepenger.oversikt.innhenting;

record EsSak(String saksnummer, String aktørId) implements Sak {

    @Override
    public String toString() {
        return "EsSak{" + "saksnummer='" + saksnummer + '\'' + '}';
    }
}
