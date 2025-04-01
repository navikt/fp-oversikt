package no.nav.foreldrepenger.oversikt.oppslag.dto;

import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.Stillingsprosent;

import java.time.LocalDate;

public record ArbeidsforholdDto(String arbeidsgiverId,
                                String arbeidsgiverIdType,
                                String arbeidsgiverNavn,
                                Stillingsprosent stillingsprosent,
                                LocalDate fom,
                                LocalDate tom) {

}