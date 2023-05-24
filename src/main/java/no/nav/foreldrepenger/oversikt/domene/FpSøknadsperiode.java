package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDate;

import no.nav.foreldrepenger.common.innsyn.SamtidigUttak;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;

public record FpSøknadsperiode(LocalDate fom, LocalDate tom, Konto konto, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                               OverføringÅrsak overføringÅrsak, Gradering gradering, Prosent samtidigUttak, Boolean flerbarnsdager,
                               MorsAktivitet morsAktivitet) {

    public UttakPeriode tilDto() {
        var kontoType = konto == null ? null : konto.tilDto();
        var utsettelse = utsettelseÅrsak() == null ? null : utsettelseÅrsak().tilDto();
        var opphold = oppholdÅrsak() == null ? null : oppholdÅrsak().tilDto();
        var overføring = overføringÅrsak() == null ? null : overføringÅrsak().tilDto();
        var ma = morsAktivitet() == null ? null : morsAktivitet().tilDto();
        var sa = samtidigUttak() == null || !samtidigUttak().merEnn0() ? null : new SamtidigUttak(samtidigUttak().decimalValue());
        //frontend vil ikke ha detaljer om gradering ved samtidigUttak
        var g = gradering() != null && sa == null ? gradering().tilDto() : null;
        return new UttakPeriode(fom(), tom(), kontoType, null, utsettelse, opphold, overføring, g, ma, sa, flerbarnsdager != null && flerbarnsdager);
    }
}
