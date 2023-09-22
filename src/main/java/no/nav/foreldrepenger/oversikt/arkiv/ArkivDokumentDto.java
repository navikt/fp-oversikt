package no.nav.foreldrepenger.oversikt.arkiv;

import java.time.LocalDateTime;

public record ArkivDokumentDto(String tittel,
                               Type type,
                               String journalpostId,
                               String dokumentId,
                               LocalDateTime mottatt) {
    public enum Type {
        INNGÅENDE_DOKUMENT,
        UTGÅENDE_DOKUMENT
    }
}