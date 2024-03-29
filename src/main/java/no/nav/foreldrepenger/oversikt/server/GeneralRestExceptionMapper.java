package no.nav.foreldrepenger.oversikt.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FpoversiktException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

@Provider
public class GeneralRestExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(GeneralRestExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof ManglerTilgangException manglerTilgangException) {
            return Response.status(manglerTilgangException.getStatusCode())
                .entity(problemDetails(manglerTilgangException))
                .build();
        }
        LOG.warn("Fikk uventet feil: {}", exception.getMessage(), exception);
        return Response.status(500).build();
    }

    static ProblemDetails problemDetails(FpoversiktException exception) {
        return new ProblemDetails(exception.getFeilKode(), exception.getStatusCode().getStatusCode(), exception.getFeilKode().getBeskrivelse());
    }
}
