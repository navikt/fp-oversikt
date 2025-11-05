package no.nav.foreldrepenger.oversikt.domene.fp;

import java.time.LocalDate;

import no.nav.foreldrepenger.kontrakter.fpoversikt.SamtidigUttak;
import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriode;
import no.nav.foreldrepenger.oversikt.domene.Prosent;

public record FpSøknadsperiode(LocalDate fom, LocalDate tom, Konto konto, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                               OverføringÅrsak overføringÅrsak, Gradering gradering, Prosent samtidigUttak, Boolean flerbarnsdager,
                               MorsAktivitet morsAktivitet) {

    public UttakPeriode tilDto(no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak brukerRolle) {
        var kontoType = konto == null ? null : konto.tilDto();
        var utsettelse = utsettelseÅrsak() == null ? null : utsettelseÅrsak().tilDto();
        var opphold = oppholdÅrsak() == null ? null : oppholdÅrsak().tilDto();
        var overføring = overføringÅrsak() == null ? null : overføringÅrsak().tilDto();
        var ma = morsAktivitet() == null ? null : morsAktivitet().tilDto();
        var sa = samtidigUttak() == null || !samtidigUttak().merEnn0() ? null : new SamtidigUttak(samtidigUttak().decimalValue());
        //frontend vil ikke ha detaljer om gradering ved samtidigUttak
        var g = gradering() != null && sa == null ? gradering().tilDto() : null;
        return new UttakPeriode(fom(), tom(), kontoType, null, utsettelse, opphold, overføring, g, ma, sa, flerbarnsdager != null && flerbarnsdager,
            brukerRolle);
    }
}
