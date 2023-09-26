package no.nav.foreldrepenger.oversikt.arkiv;

public record EnkelJournalpost(String saksnummer,
                               DokumentType type,
                               DokumentTypeId hovedtype) {
    public enum DokumentType {
        INNGÅENDE_DOKUMENT,
        UTGÅENDE_DOKUMENT
    }
}
