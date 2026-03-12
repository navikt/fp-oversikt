package no.nav.foreldrepenger.oversikt.oppslag.svp;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Kjønn;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Navn;

record SvpPersonopplysningerDto(@NotNull Fødselsnummer fnr, @NotNull LocalDate fødselsdato, @NotNull Kjønn kjønn, @NotNull Navn navn,
                                @NotNull List<EksternArbeidsforholdDto> arbeidsforhold) {
}

