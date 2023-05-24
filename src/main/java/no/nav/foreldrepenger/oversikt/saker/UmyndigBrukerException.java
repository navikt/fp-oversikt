package no.nav.foreldrepenger.oversikt.saker;

import no.nav.vedtak.exception.VLException;

public class UmyndigBrukerException extends VLException {

    public UmyndigBrukerException() {
        super("UMYNDIG_BRUKER", "Innlogget bruker er under myndighetsalder", null);
    }
}
