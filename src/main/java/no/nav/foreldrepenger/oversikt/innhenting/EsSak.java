package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDateTime;
import java.util.Set;

public record EsSak(String saksnummer,
                    String aktørId,
                    FamilieHendelse familieHendelse,
                    boolean avsluttet,
                    Set<Aksjonspunkt> aksjonspunkt,
                    Set<Søknad> søknader,
                    Set<Vedtak> vedtak) implements Sak {

    public record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
    }

    public record Vedtak(LocalDateTime vedtakstidspunkt) {
    }

    @Override
    public String toString() {
        return "EsSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", avsluttet=" + avsluttet + ", aksjonspunkt="
            + aksjonspunkt + ", søknader=" + søknader + ", vedtak=" + vedtak + '}';
    }
}
