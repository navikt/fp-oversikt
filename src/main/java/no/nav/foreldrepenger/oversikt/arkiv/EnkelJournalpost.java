package no.nav.foreldrepenger.oversikt.arkiv;

import java.util.Set;

import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;

public record EnkelJournalpost(String saksnummer, Set<DokumentType> dokumentTypeId, Journalposttype journalposttype, KildeSystem kildeSystem) {
    public enum Journalposttype {
        UTGÅENDE, INNGÅENDE, NOTAT

    }

    public enum KildeSystem {
        FPTILBAKE, ANNET
    }
}
