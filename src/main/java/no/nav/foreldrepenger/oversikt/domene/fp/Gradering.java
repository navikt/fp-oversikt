package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.common.innsyn.Aktivitet;
import no.nav.foreldrepenger.oversikt.domene.Prosent;

public record Gradering(Prosent prosent, UttakAktivitet uttakAktivitet) {
    public no.nav.foreldrepenger.common.innsyn.Gradering tilDto() {
        var arbeidsgiver = uttakAktivitet().arbeidsgiver();
        return new no.nav.foreldrepenger.common.innsyn.Gradering(prosent().decimalValue(),
            new Aktivitet(uttakAktivitet().type().tilDto(), arbeidsgiver == null ? null : arbeidsgiver.tilDto()));
    }
}
