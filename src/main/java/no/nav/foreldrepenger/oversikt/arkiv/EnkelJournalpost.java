package no.nav.foreldrepenger.oversikt.arkiv;


import no.nav.foreldrepenger.common.domain.felles.DokumentType;

public record EnkelJournalpost(String journalpostId,
                               String fagsaksystem,
                               String eksternReferanse,
                               String saksnummer,
                               JournalpostType type,
                               Bruker bruker,
                               DokumentType hovedtype) {

    public boolean erInfotrygdSak() {
        return "IT01".equalsIgnoreCase(fagsaksystem);
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
