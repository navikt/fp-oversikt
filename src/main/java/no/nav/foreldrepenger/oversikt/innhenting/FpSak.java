package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record FpSak(String saksnummer, String aktørId, FamilieHendelse familieHendelse, Set<Vedtak> vedtakene, String oppgittAnnenPart) implements Sak {

    public record Vedtak(LocalDateTime vedtakstidspunkt, List<Uttaksperiode> uttaksperioder, Dekningsgrad dekningsgrad) {
        public enum Dekningsgrad {
            ÅTTI,
            HUNDRE
        }
    }

    public record Uttaksperiode(LocalDate fom, LocalDate tom, Resultat resultat) {

        public record Resultat(Type type) {

            public enum Type {
                INNVILGET,
                AVSLÅTT
            }
        }
    }

    @Override
    public String toString() {
        return "FpSak{" + "saksnummer='" + saksnummer + '\'' + ", vedtakene=" + vedtakene + ", familieHendelse=" + familieHendelse + '}';
    }
}
