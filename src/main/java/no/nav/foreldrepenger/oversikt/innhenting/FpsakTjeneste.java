package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.UUID;

public interface FpsakTjeneste {
    SakDto hentSak(UUID behandlingUuid);

    record SakDto(String saksnummer, Status status, YtelseType ytelseType, String aktørId) {

        public enum Status {
            AVSLUTTET,
            ÅPEN
        }

        public enum YtelseType {
            FORELDREPENGER,
            SVANGERSKAPSPENGER,
            ENGANGSSTØNAD
        }
    }
}
