package no.nav.foreldrepenger.oversikt.arkiv;


public record EnkelJournalpost(String journalpostId,
                               String eksternReferanse,
                               String saksnummer,
                               DokumentType type,
                               Bruker bruker,
                               DokumentTypeId hovedtype) {
    public enum DokumentType {
        INNGÅENDE_DOKUMENT,
        UTGÅENDE_DOKUMENT
    }

    public record Bruker(Type type, String ident) {
        public enum Type {
            AKTØRID,
            FNR
        }

        @Override
        public String toString() {
            return "Bruker{" + "type=" + type + '}';
        }
    }
}
