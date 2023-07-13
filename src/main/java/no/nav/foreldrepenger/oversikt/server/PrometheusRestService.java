package no.nav.foreldrepenger.oversikt.server;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/metrics")
@Produces(TEXT_PLAIN)
@ApplicationScoped
public class PrometheusRestService {

    @GET
    @Operation(hidden = true)
    @Path("/prometheus")
    public String prometheus() {
        return REGISTRY.scrape();
    }
}
