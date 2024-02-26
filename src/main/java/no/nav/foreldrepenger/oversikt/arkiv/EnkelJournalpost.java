package no.nav.foreldrepenger.oversikt.arkiv;


import no.nav.foreldrepenger.common.domain.felles.DokumentType;

public record EnkelJournalpost(String journalpostId,
                               String eksternReferanse,
                               String saksnummer,
                               Type type,
                               Bruker bruker,
                               DokumentType hovedtype) {
    public enum Type {
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
