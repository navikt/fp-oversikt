package no.nav.foreldrepenger.oversikt.server;

import no.nav.foreldrepenger.oversikt.saker.UmyndigBrukerException;
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
        if (exception instanceof UmyndigBrukerException umyndigBrukerException) {
            var status = Response.Status.FORBIDDEN;
            return Response.status(status)
                .entity(problemDetails(umyndigBrukerException, status.getStatusCode()))
                .build();
        }
        LOG.warn("FP-OVERSIKT fikk feil", exception);
        return Response.status(500).build();
    }

    static ProblemDetails problemDetails(VLException vlException, int feilkode) {
        return new ProblemDetails(vlException.getKode(), feilkode, vlException.getFeilmelding());
    }

}
