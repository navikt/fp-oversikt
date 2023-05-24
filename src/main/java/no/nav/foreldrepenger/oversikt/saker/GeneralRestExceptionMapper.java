package no.nav.foreldrepenger.oversikt.saker;

import no.nav.foreldrepenger.oversikt.server.ProblemDetails;
import no.nav.vedtak.exception.VLException;

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
        LOG.warn("hei fra error", exception);
        if (exception instanceof UmyndigBrukerException umyndigBrukerException) {
            var status = Response.Status.FORBIDDEN;
            return Response.status(status)
                .entity(problemDetails(umyndigBrukerException, status.getStatusCode()))
                .build();
        }
        return Response.status(500).entity(exception).build();
    }

    public static ProblemDetails problemDetails(VLException vlException, int feilkode) {
        return new ProblemDetails(vlException.getKode(), feilkode, vlException.getFeilmelding());
    }

    public static class UmyndigBrukerException extends VLException {

        protected UmyndigBrukerException() {
            super("UMYNDIG_BRUKER", "Innlogget bruker er under myndighetsalder", null);
        }
    }
}
