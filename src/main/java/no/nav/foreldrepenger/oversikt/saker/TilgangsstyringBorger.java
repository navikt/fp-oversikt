package no.nav.foreldrepenger.oversikt.saker;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

public class TilgangsstyringBorger {

    public static void sjekkAtKallErFraBorger() {
        var kontekst = KontekstHolder.getKontekst();
        if (erBorger(kontekst)) {
            return;
        }
        throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_IKKE_EKSTERN);
    }

    private static boolean erBorger(Kontekst kontekst) {
        if (kontekst == null) {
            return false;
        }
        return IdentType.EksternBruker.equals(kontekst.getIdentType());
    }
}
