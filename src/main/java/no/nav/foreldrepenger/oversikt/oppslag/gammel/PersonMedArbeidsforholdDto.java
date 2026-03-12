package no.nav.foreldrepenger.oversikt.oppslag.gammel;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;

import java.util.List;

public record PersonMedArbeidsforholdDto(@NotNull PersonDto person, @NotNull List<EksternArbeidsforholdDto> arbeidsforhold) {
}
