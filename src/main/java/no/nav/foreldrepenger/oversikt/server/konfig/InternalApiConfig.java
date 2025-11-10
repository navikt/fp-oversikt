package no.nav.foreldrepenger.oversikt.server.konfig;

import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.ApplicationPath;
import no.nav.foreldrepenger.oversikt.server.HealtCheckRest;
import no.nav.foreldrepenger.oversikt.server.PrometheusRestService;

@ApplicationPath(InternalApiConfig.API_URI)
public class InternalApiConfig extends ResourceConfig {

    public static final String API_URI = "/internal";

    public InternalApiConfig() {
        register(HealtCheckRest.class);
        register(PrometheusRestService.class);
    }
}
