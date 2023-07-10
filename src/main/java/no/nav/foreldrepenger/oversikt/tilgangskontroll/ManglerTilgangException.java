package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import jakarta.ws.rs.core.Response;

public class ManglerTilgangException extends FpoversiktException {

    public ManglerTilgangException(FeilKode feilKode, Throwable cause) {
        super(feilKode, Response.Status.FORBIDDEN, cause);
    }

    public ManglerTilgangException(FeilKode feilKode) {
        super(feilKode, Response.Status.FORBIDDEN, null);
    }

}
