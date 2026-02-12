package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.kontrakter.fpoversikt.Inntektskilde;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Beregningsgrunnlag(LocalDate skjæringsTidspunkt, List<BeregningsAndel> beregningsAndeler,
                                 List<BeregningAktivitetStatus> beregningAktivitetStatuser, BigDecimal grunnbeløp) {

    public record BeregningsAndel(AktivitetStatus aktivitetStatus, BigDecimal fastsattPrÅr, InntektsKilde inntektsKilde,
                                  Arbeidsforhold arbeidsforhold, BigDecimal dagsatsArbeidsgiver, BigDecimal dagsatsSøker) {
    }

    public record Arbeidsforhold(String arbeidsgiverIdent, String arbeidsgivernavn, BigDecimal refusjonPrMnd) {
    }

    public record BeregningAktivitetStatus(AktivitetStatus aktivitetStatus, String hjemmel) {
    }

    public enum InntektsKilde {
        INNTEKTSMELDING,
        A_INNTEKT,
        VEDTAK_ANNEN_YTELSE,
        SKJØNNSFASTSATT,
        PENSJONSGIVENDE_INNTEKT;

        public Inntektskilde tilDto() {
            return switch (this) {
                case INNTEKTSMELDING -> Inntektskilde.INNTEKTSMELDING;
                case A_INNTEKT -> Inntektskilde.A_INNTEKT;
                case VEDTAK_ANNEN_YTELSE -> Inntektskilde.VEDTAK_ANNEN_YTELSE;
                case SKJØNNSFASTSATT -> Inntektskilde.SKJØNNSFASTSATT;
                case PENSJONSGIVENDE_INNTEKT -> Inntektskilde.PGI;
            };
        }
    }

    public no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag tilDto() {
        List<no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningsAndel> andeler =
            beregningsAndeler == null ? List.of() : beregningsAndeler.stream().map(this::mapAndel).toList();
        List<no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningAktivitetStatus> statuser =
            beregningAktivitetStatuser == null ? List.of() : beregningAktivitetStatuser.stream().map(this::mapStatusMedHjemmel).toList();
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag(skjæringsTidspunkt, andeler, statuser, grunnbeløp);
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningAktivitetStatus mapStatusMedHjemmel(BeregningAktivitetStatus status) {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningAktivitetStatus(status.aktivitetStatus.tilDto(),
            status.hjemmel);
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningsAndel mapAndel(BeregningsAndel andel) {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningsAndel(andel.aktivitetStatus.tilDto(),
            andel.fastsattPrÅr, andel.inntektsKilde.tilDto(), mapArbeidsforhold(andel.arbeidsforhold), andel.dagsatsArbeidsgiver,
            andel.dagsatsSøker);
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.Arbeidsforhold mapArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        if (arbeidsforhold == null) {
            return null;
        }
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.Arbeidsforhold(arbeidsforhold.arbeidsgiverIdent,
            arbeidsforhold.arbeidsgivernavn, arbeidsforhold.refusjonPrMnd);
    }


}

