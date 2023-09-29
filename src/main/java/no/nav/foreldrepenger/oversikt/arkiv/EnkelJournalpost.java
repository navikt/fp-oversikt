package no.nav.foreldrepenger.oversikt.arkiv;


import no.nav.foreldrepenger.common.domain.Fødselsnummer;

public record EnkelJournalpost(String saksnummer,
                               String eksternReferanse,
                               DokumentType type,
                               Fødselsnummer fødselsnummerAvsenderMottaker,
                               DokumentTypeId hovedtype) {
    public enum DokumentType {
        INNGÅENDE_DOKUMENT,
        UTGÅENDE_DOKUMENT
    }
}
