package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Beløp;

public record Inntektsmelding(Arbeidsgiver arbeidsgiver, LocalDateTime innsendingstidspunkt, Beløp inntekt) {
}