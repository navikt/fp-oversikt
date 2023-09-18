package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Beløp;

public record Inntektsmelding(String journalpostId, Arbeidsgiver arbeidsgiver, LocalDateTime innsendingstidspunkt, Beløp inntekt,
                              LocalDateTime mottattTidspunkt) {
}
