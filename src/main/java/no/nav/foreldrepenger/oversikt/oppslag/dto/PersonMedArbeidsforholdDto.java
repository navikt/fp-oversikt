package no.nav.foreldrepenger.oversikt.oppslag.dto;

import java.util.List;

public record PersonMedArbeidsforholdDto(PersonDto person, List<ArbeidsforholdDto> arbeidsforhold) {
}
