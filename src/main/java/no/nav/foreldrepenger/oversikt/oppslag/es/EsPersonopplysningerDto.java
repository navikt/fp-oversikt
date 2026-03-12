package no.nav.foreldrepenger.oversikt.oppslag.es;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Kjønn;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Navn;

record EsPersonopplysningerDto(@NotNull Fødselsnummer fnr, @NotNull LocalDate fødselsdato, @NotNull Kjønn kjønn, @NotNull Navn navn) {
}

