package no.nav.foreldrepenger.oversikt.arkiv;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record DokumentDto(String tittel,
                          @NotNull JournalpostType type,
                          @NotNull String saksnummer,
                          @NotNull String journalpostId,
                          @NotNull String dokumentId,
                          @NotNull LocalDateTime mottatt) {
}
