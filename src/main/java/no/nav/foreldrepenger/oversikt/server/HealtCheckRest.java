package no.nav.foreldrepenger.oversikt.server;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/health")
@ApplicationScoped
public class HealtCheckRest {

    @GET
    @Path("/isAlive")
    public Response isAlive() {
        return Response.ok().build();
    }

    @GET
    @Path("/isReady")
    public Response isReady() {
        return Response.ok().build();
    }

    @GET
    @Path("/preStop")
    public Response preStop() {
        return Response.ok().build();
    }
}
