package no.nav.foreldrepenger.oversikt.arkiv;

public record DokumentDto(byte[] innhold, String contentType, String contentDisp) {
}
