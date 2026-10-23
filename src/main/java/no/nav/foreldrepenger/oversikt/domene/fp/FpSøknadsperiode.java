package no.nav.foreldrepenger.oversikt.domene.fp;

import java.time.LocalDate;

import no.nav.foreldrepenger.oversikt.domene.Prosent;

public record FpSøknadsperiode(LocalDate fom, LocalDate tom, Konto konto, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                               OverføringÅrsak overføringÅrsak, Gradering gradering, Prosent samtidigUttak, Boolean flerbarnsdager,
                               MorsAktivitet morsAktivitet) {
}
