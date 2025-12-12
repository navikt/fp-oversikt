package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.kontrakter.fpoversikt.Inntektskilde;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Beregningsgrunnlag(LocalDate skjæringsTidspunkt, List<BeregningsAndel> beregningsAndeler,
                                 List<BeregningAktivitetStatus> beregningAktivitetStatuser) {

    public record BeregningsAndel(AktivitetStatus aktivitetStatus, BigDecimal fastsattPrÅr, InntektsKilde inntektsKilde,
                                  Arbeidsforhold arbeidsforhold, BigDecimal dagsatsArbeidsgiver, BigDecimal dagsatsSøker) {
    }

    public record Arbeidsforhold(String arbeidsgiverIdent, BigDecimal refusjonPrMnd) {
    }

    public record BeregningAktivitetStatus(AktivitetStatus aktivitetStatus, String hjemmel) {
    }

    public enum InntektsKilde {
        INNTEKTSMELDING,
        A_INNTEKT,
        VEDTAK_ANNEN_YTELSE,
        SKJØNNSFASTSATT,
        PGI // Pensjonsgivendeinntekt
    }

    public no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag tilDto() {
        List<no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningsAndel> andeler =
            beregningsAndeler == null ? List.of() : beregningsAndeler.stream().map(this::mapAndel).toList();
        List<no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningAktivitetStatus> statuser =
            beregningAktivitetStatuser == null ? List.of() : beregningAktivitetStatuser.stream().map(this::mapStatusMedHjemmel).toList();
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag(skjæringsTidspunkt, andeler, statuser);
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningAktivitetStatus mapStatusMedHjemmel(BeregningAktivitetStatus status) {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningAktivitetStatus(mapAktivitetstatus(status.aktivitetStatus), status.hjemmel);
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningsAndel mapAndel(BeregningsAndel andel) {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.BeregningsAndel(mapAktivitetstatus(andel.aktivitetStatus),
            andel.fastsattPrÅr, mapInntektskilde(andel.inntektsKilde), mapArbeidsforhold(andel.arbeidsforhold), andel.dagsatsArbeidsgiver,
            andel.dagsatsSøker);
    }

    private no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.Arbeidsforhold mapArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        if (arbeidsforhold == null) {
            return null;
        }
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Beregningsgrunnlag.Arbeidsforhold(arbeidsforhold.arbeidsgiverIdent,
            arbeidsforhold.refusjonPrMnd);
    }

    private Inntektskilde mapInntektskilde(InntektsKilde inntektsKilde) {
        return switch (inntektsKilde) {
            case INNTEKTSMELDING -> Inntektskilde.INNTEKTSMELDING;
            case A_INNTEKT -> Inntektskilde.A_INNTEKT;
            case VEDTAK_ANNEN_YTELSE -> Inntektskilde.VEDTAK_ANNEN_YTELSE;
            case SKJØNNSFASTSATT -> Inntektskilde.SKJØNNSFASTSATT;
            case PGI -> Inntektskilde.PGI;
        };
    }

    private no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus mapAktivitetstatus(AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.ARBEIDSTAKER;
            case DAGPENGER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.DAGPENGER;
            case FRILANSER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINERT_AT_FL -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KOMBINERT_FL_SN;
            case KOMBINERT_AT_FL_SN -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KUN_YTELSE;
        };
    }

}

