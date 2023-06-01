package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDateTime;
import java.util.Set;

public record SvpSak(String saksnummer,
                     String aktørId,
                     FamilieHendelse familieHendelse,
                     Status status,
                     Set<Aksjonspunkt> aksjonspunkt,
                     Set<Søknad> søknader,
                     Set<Vedtak> vedtak) implements Sak {

    public record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
    }

    public record Vedtak(LocalDateTime vedtakstidspunkt) {
    }

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", aksjonspunkt="
            + aksjonspunkt + ", søknader=" + søknader + ", vedtak=" + vedtak + '}';
    }
}
