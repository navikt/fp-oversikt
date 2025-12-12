package no.nav.foreldrepenger.oversikt.domene.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Beregningsgrunnlag(LocalDate skjæringsTidspunkt, List<BeregningsAndel> beregningsAndeler, List<BeregningAktivitetStatus> beregningAktivitetStatuser) {

    public record BeregningsAndel(AktivitetStatus aktivitetStatus, BigDecimal fastsattPrÅr, InntektsKilde inntektsKilde,
                           Arbeidsforhold arbeidsforhold, BigDecimal dagsatsArbeidsgiver, BigDecimal dagsatsSøker) {}

    public record Arbeidsforhold(String arbeidsgiverIdent, BigDecimal refusjonPrMnd) {}

    public record BeregningAktivitetStatus(AktivitetStatus aktivitetStatus, String hjemmel) {}

    public enum InntektsKilde {
        INNTEKTSMELDING,
        A_INNTEKT,
        VEDTAK_ANNEN_YTELSE,
        SKJØNNSFASTSATT,
        PGI // Pensjonsgivendeinntekt
    }
}

