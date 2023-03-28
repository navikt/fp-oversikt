package no.nav.foreldrepenger.oversikt.server;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/health")
@ApplicationScoped
public class HealtCheckRest {

    private static final Logger LOG = LoggerFactory.getLogger(HealtCheckRest.class);

    @GET
    @Path("/isAlive")
    public Response isAlive() {
        LOG.info("isAlive kalt {}");
        return Response.ok().build();
    }

    @GET
    @Path("/isReady")
    public Response isReady() {
        LOG.info("isReady kalt");
        return Response.ok().build();
    }

    @GET
    @Path("/preStop")
    public Response preStop() {
        LOG.info("preStop kalt");
        return Response.ok().build();
    }
}
