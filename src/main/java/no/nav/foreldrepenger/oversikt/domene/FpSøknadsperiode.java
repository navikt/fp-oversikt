package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDate;

import no.nav.foreldrepenger.common.innsyn.UttakPeriode;

public record FpSøknadsperiode(LocalDate fom, LocalDate tom) {

    public UttakPeriode tilDto() {
        //TODO
        return new UttakPeriode(fom(), tom(), null, null, null, null, null, null, null, null, false);
    }
}
