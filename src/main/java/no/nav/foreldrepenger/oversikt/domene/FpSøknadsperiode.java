package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDate;

import no.nav.foreldrepenger.common.innsyn.SamtidigUttak;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;

public record FpSøknadsperiode(LocalDate fom, LocalDate tom, Konto konto, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                               OverføringÅrsak overføringÅrsak, Gradering gradering, Prosent samtidigUttak, Boolean flerbarnsdager,
                               MorsAktivitet morsAktivitet) {

    public UttakPeriode tilDto() {
        var kontoType = konto == null ? null : konto.tilDto();
        var utsettelse = utsettelseÅrsak() == null ? null : switch (utsettelseÅrsak()) {
            case HV_ØVELSE -> no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.HV_ØVELSE;
            case ARBEID -> no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.ARBEID;
            case LOVBESTEMT_FERIE -> no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.LOVBESTEMT_FERIE;
            case SØKER_SYKDOM -> no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.BARN_INNLAGT;
            case NAV_TILTAK -> no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.FRI;
        };
        var opphold = oppholdÅrsak() == null ? null : switch (oppholdÅrsak()) {
            case MØDREKVOTE_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
            case FEDREKVOTE_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
            case FELLESPERIODE_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
            case FORELDREPENGER_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.OppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
        };
        var overføring = overføringÅrsak() == null ? null : switch (overføringÅrsak()) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> no.nav.foreldrepenger.common.innsyn.OverføringÅrsak.ALENEOMSORG;
        };
        var ma = morsAktivitet() == null ? null : morsAktivitet().tilDto();
        var sa = samtidigUttak() == null ? null : new SamtidigUttak(samtidigUttak().decimalValue());
        var g = gradering() == null ? null : gradering().tilDto();
        return new UttakPeriode(fom(), tom(), kontoType, null, utsettelse, opphold, overføring, g, ma, sa, flerbarnsdager != null && flerbarnsdager);
    }
}
