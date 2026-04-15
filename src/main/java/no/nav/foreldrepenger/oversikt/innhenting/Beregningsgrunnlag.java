package no.nav.foreldrepenger.oversikt.innhenting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record Beregningsgrunnlag(LocalDate skjæringstidspunkt, List<BeregningsAndel> beregningsAndeler,
                          List<BeregningAktivitetStatus> beregningAktivitetStatuser, BigDecimal grunnbeløp) {

    record BeregningsAndel(AktivitetStatus aktivitetStatus, BigDecimal fastsattPrÅr, InntektsKilde inntektsKilde, Arbeidsforhold arbeidsforhold,
                           BigDecimal dagsatsArbeidsgiver, BigDecimal dagsatsSøker) {
    }

    record Arbeidsforhold(String arbeidsgiverIdent, String arbeidsgivernavn, BigDecimal refusjonPrMnd) {
    }

    // Lar hjemmel være string til vi vet om vi skal ha det med
    record BeregningAktivitetStatus(AktivitetStatus aktivitetStatus, String hjemmel) {
    }

    enum InntektsKilde {
        INNTEKTSMELDING,
        A_INNTEKT,
        VEDTAK_ANNEN_YTELSE,
        SKJØNNSFASTSATT,
        PENSJONSGIVENDE_INNTEKT,
    }

}
