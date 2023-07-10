package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import jakarta.ws.rs.core.Response;

public class FpoversiktException extends RuntimeException {

    private final FeilKode feilKode;
    private final Response.Status statusCode;

    public FpoversiktException(FeilKode feilKode, Response.Status statusCode, Throwable cause) {
        super(feilKode.name() + ":" + feilKode.getBeskrivelse(), cause);
        this.feilKode = feilKode;
        this.statusCode = statusCode;
    }

    public FeilKode getFeilKode() {
        return feilKode;
    }

    public Response.Status getStatusCode() {
        return statusCode;
    }
}
