package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDate;

import no.nav.foreldrepenger.common.innsyn.UttakPeriode;

public record FpSøknadsperiode(LocalDate fom, LocalDate tom, Konto konto) {

    public UttakPeriode tilDto() {
        //TODO
        var kontoType = konto == null ? null : konto.tilDto();
        return new UttakPeriode(fom(), tom(), kontoType, null, null, null, null, null, null, null, false);
    }
}
