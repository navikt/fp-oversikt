package no.nav.foreldrepenger.oversikt.oppslag.mapper;

import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.EksternArbeidsforhold;
import no.nav.foreldrepenger.oversikt.oppslag.dto.ArbeidsforholdDto;

import java.util.List;
import java.util.Objects;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

public class ArbeidsforholdDtoMapper {

    private ArbeidsforholdDtoMapper() {
        // hide public constructor
    }

    public static List<ArbeidsforholdDto> tilArbeidsforholdDto(List<EksternArbeidsforhold> eksternArbeidsforholds) {
        return safeStream(eksternArbeidsforholds)
                .filter(Objects::nonNull)
                .map(ArbeidsforholdDtoMapper::tilArbeidsforholdDto)
                .toList();
    }

    private static ArbeidsforholdDto tilArbeidsforholdDto(EksternArbeidsforhold eksternArbeidsforhold) {
        return new ArbeidsforholdDto(
                eksternArbeidsforhold.arbeidsgiverId(),
                eksternArbeidsforhold.arbeidsgiverIdType(),
                eksternArbeidsforhold.arbeidsgiverNavn(),
                eksternArbeidsforhold.stillingsprosent(),
                eksternArbeidsforhold.from(),
                eksternArbeidsforhold.to().isPresent() ? eksternArbeidsforhold.to().get() : null
        );
    }
}
