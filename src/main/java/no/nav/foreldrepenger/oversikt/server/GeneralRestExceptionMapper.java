package no.nav.foreldrepenger.oversikt.server;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FpoversiktException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GeneralRestExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(GeneralRestExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof ManglerTilgangException manglerTilgangException) {
            if (FeilKode.IKKE_TILGANG_UMYNDIG.equals(manglerTilgangException.getFeilKode()) {
                return Response.noContent().build();
            }

            return Response.status(manglerTilgangException.getStatusCode())R
                .entity(problemDetails(manglerTilgangException))
                .build();
        }
        LOG.warn("FP-OVERSIKT fikk feil", exception);
        return Response.status(500).build();
    }

    static ProblemDetails problemDetails(FpoversiktException exception) {
        return new ProblemDetails(exception.getFeilKode(), exception.getStatusCode().getStatusCode(), exception.getFeilKode().getBeskrivelse());
    }
}
