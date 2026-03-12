package no.nav.foreldrepenger.oversikt.oppslag.fp;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Kjønn;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Navn;

record FpPersonopplysningerDto(@NotNull Fødselsnummer fnr, @NotNull LocalDate fødselsdato, @NotNull Kjønn kjønn, @NotNull Navn navn, @NotNull boolean erGift,
                               @NotNull List<BarnDto> barn, @NotNull List<EksternArbeidsforholdDto> arbeidsforhold) {

    record BarnDto(@NotNull Fødselsnummer fnr, @NotNull LocalDate fødselsdato, LocalDate dødsdato, Navn navn, @NotNull Kjønn kjønn,
                   AnnenForelderDto annenPart) {
    }

    record AnnenForelderDto(@NotNull Fødselsnummer fnr, @NotNull Navn navn, LocalDate fødselsdato) {
    }
}

