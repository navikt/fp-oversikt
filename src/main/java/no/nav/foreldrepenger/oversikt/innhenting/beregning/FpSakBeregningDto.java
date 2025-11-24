package no.nav.foreldrepenger.oversikt.innhenting.beregning;

import no.nav.foreldrepenger.oversikt.domene.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FpSakBeregningDto(LocalDate skjæringsTidspunkt, List<BeregningsAndel> beregningsAndeler, List<BeregningAktivitetStatus> beregningAktivitetStatuser) {

    // TODO: bruke denne AktivtetStatus?
    public record BeregningsAndel(Object aktivitetStatus, BigDecimal fastsattPrMnd, InntektsKilde inntektsKilde, Arbeidsforhold arbeidsforhold, BigDecimal dagsats) {}

    public record Arbeidsforhold(String arbeidsgiverIdent, BigDecimal refusjonPrMnd) {}

    public record BeregningAktivitetStatus(Object aktivitetStatus, Object hjemmel) {}

    public enum InntektsKilde {
        INNTEKTSMELDING,
        A_INNTEKT,
        VEDTAK_ANNEN_YTELSE,
        SKJØNNSFASTSATT,
        PGI // Pensjonsgivendeinntekt
    }
}
