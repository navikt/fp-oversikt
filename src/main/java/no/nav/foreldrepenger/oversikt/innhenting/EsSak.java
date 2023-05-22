package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDateTime;
import java.util.Set;

public record EsSak(String saksnummer,
                    String aktørId,
                    FamilieHendelse familieHendelse,
                    Status status,
                    Set<Aksjonspunkt> aksjonspunkt,
                    Set<Søknad> søknader) implements Sak {

    public record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
    }

    @Override
    public String toString() {
        return "EsSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", aksjonspunkt="
            + aksjonspunkt + ", søknader=" + søknader + '}';
    }
}
