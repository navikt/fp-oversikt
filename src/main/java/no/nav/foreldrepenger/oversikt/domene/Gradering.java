package no.nav.foreldrepenger.oversikt.domene;

import no.nav.foreldrepenger.common.innsyn.Aktivitet;

public record Gradering(Prosent prosent, UttakAktivitet uttakAktivitet) {
    public no.nav.foreldrepenger.common.innsyn.Gradering tilDto() {
        var arbeidsgiver = uttakAktivitet().arbeidsgiver();
        return new no.nav.foreldrepenger.common.innsyn.Gradering(prosent().decimalValue(),
            new Aktivitet(uttakAktivitet().type().tilDto(), arbeidsgiver == null ? null : arbeidsgiver.tilDto()));
    }
}
