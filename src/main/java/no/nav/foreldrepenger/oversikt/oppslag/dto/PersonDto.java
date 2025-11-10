package no.nav.foreldrepenger.oversikt.oppslag.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.kontrakter.felles.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;

public record PersonDto(AktørId aktørid,
                        @NotNull Fødselsnummer fnr,
                        @NotNull LocalDate fødselsdato,
                        @NotNull Navn navn,
                        @NotNull Kjønn kjønn,
                        Målform målform,
                        Bankkonto bankkonto,
                        Sivilstand sivilstand,
                        @NotNull List<BarnDto> barn) {

    public record BarnDto(@NotNull Fødselsnummer fnr,
                          @NotNull LocalDate fødselsdato,
                          LocalDate dødsdato,
                          Navn navn,
                          @NotNull Kjønn kjønn,
                          AnnenForelderDto annenPart) {
    }

    public record AnnenForelderDto(@NotNull Fødselsnummer fnr, @NotNull Navn navn, LocalDate fødselsdato) {
    }

}
