package no.nav.foreldrepenger.oversikt.oppslag.dto;

import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;

import java.util.List;

public record PersonMedArbeidsforholdDto(PersonDto person, List<EksternArbeidsforholdDto> arbeidsforhold) {
}
