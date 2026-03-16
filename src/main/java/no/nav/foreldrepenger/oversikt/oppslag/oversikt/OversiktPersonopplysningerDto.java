package no.nav.foreldrepenger.oversikt.oppslag.oversikt;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Navn;

record OversiktPersonopplysningerDto(@NotNull Fødselsnummer fnr, @NotNull LocalDate fødselsdato, @NotNull Navn navn, String kontonummer,
                                     @NotNull boolean harArbeidsforhold, @NotNull List<OversiktBarnDto> barn) {

    public record OversiktBarnDto(@NotNull Fødselsnummer fnr, @NotNull LocalDate fødselsdato, LocalDate dødsdato, Navn navn,
                                  String annenPartFornavn) {
    }
}
