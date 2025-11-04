package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.kontrakter.fpoversikt.Aktivitet;
import no.nav.foreldrepenger.oversikt.domene.Prosent;

public record Gradering(Prosent prosent, UttakAktivitet uttakAktivitet) {
    public no.nav.foreldrepenger.kontrakter.fpoversikt.Gradering tilDto() {
        var arbeidsgiver = uttakAktivitet().arbeidsgiver();
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Gradering(prosent().decimalValue(),
            new Aktivitet(uttakAktivitet().type().tilDto(), arbeidsgiver == null ? null : arbeidsgiver.tilDto(), null));
    }
}
