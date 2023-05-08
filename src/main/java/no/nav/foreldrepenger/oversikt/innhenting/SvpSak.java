package no.nav.foreldrepenger.oversikt.innhenting;

record SvpSak(String saksnummer, String aktørId) implements Sak {

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + '}';
    }
}
