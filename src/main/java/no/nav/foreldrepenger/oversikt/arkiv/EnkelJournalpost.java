package no.nav.foreldrepenger.oversikt.arkiv;

import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;

import java.util.Set;

public record EnkelJournalpost(String saksnummer, Set<DokumentType> dokumentTypeId) {
}
