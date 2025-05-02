package no.nav.foreldrepenger.oversikt.arkiv;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DokumentDto(String tittel,
                          @NotNull JournalpostType type,
                          String saksnummer,
                          String journalpostId,
                          String dokumentId,
                          @NotNull LocalDateTime mottatt) {
}
