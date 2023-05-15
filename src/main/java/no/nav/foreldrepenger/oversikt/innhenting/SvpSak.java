package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.Set;

public record SvpSak(String saksnummer,
                     String aktørId,
                     FamilieHendelse familieHendelse,
                     Status status,
                     Set<Aksjonspunkt> aksjonspunkt,
                     Set<Egenskap> egenskaper) implements Sak {

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", aksjonspunkt="
            + aksjonspunkt + ", egenskaper=" + egenskaper + '}';
    }
}
