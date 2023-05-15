package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDateTime;
import java.util.Set;

public record SvpSak(String saksnummer,
                     String aktørId,
                     FamilieHendelse familieHendelse,
                     Status status,
                     Set<Aksjonspunkt> aksjonspunkt,
                     Set<Søknad> søknader) implements Sak {

    public record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
    }

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", aksjonspunkt="
            + aksjonspunkt + ", søknader=" + søknader + '}';
    }
}
