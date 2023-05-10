package no.nav.foreldrepenger.oversikt.innhenting;

public record SvpSak(String saksnummer, String aktørId, FamilieHendelse familieHendelse) implements Sak {

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + '}';
    }
}
