package no.nav.foreldrepenger.oversikt.innhenting;

public record EsSak(String saksnummer, String aktørId, FamilieHendelse familieHendelse) implements Sak {

    @Override
    public String toString() {
        return "EsSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + '}';
    }
}
