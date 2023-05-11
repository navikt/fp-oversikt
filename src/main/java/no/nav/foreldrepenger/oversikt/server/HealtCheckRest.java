package no.nav.foreldrepenger.oversikt.server;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.log.metrics.LivenessAware;
import no.nav.vedtak.log.metrics.ReadinessAware;

@Path("/health")
@ApplicationScoped
public class HealtCheckRest {

    private static final Logger LOG = LoggerFactory.getLogger(HealtCheckRest.class);

    private List<LivenessAware> live;
    private List<ReadinessAware> ready;
    private ApplicationServiceStarter starter;

    @Inject
    public HealtCheckRest(ApplicationServiceStarter starter,
                          @Any Instance<LivenessAware> live,
                          @Any Instance<ReadinessAware> ready) {
        this.live = live.stream().toList();
        this.ready = ready.stream().toList();
        this.starter = starter;
    }

    HealtCheckRest() {
        //CDI
    }

    @GET
    @Path("/isAlive")
    public Response isAlive() {
        if (live.stream().allMatch(LivenessAware::isAlive)) {
            return Response.ok().build();
        }
        LOG.info("/isAlive NOK.");
        return Response.serverError().build();
    }

    @GET
    @Path("/isReady")
    public Response isReady() {
        if (ready.stream().allMatch(ReadinessAware::isReady)) {
            return Response.ok().build();
        }
        LOG.info("/isReady NOK.");
        return Response.status(SERVICE_UNAVAILABLE).build();
    }

    @GET
    @Path("/preStop")
    public Response preStop() {
        starter.stopServices();
        return Response.ok().build();
    }
}
