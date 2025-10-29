package no.nav.foreldrepenger.oversikt.server.app;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import no.nav.foreldrepenger.oversikt.server.HealtCheckRest;
import no.nav.foreldrepenger.oversikt.server.PrometheusRestService;
import no.nav.foreldrepenger.oversikt.uttak.UttakRest;

@ApplicationPath(InternalApiConfig.API_URI)
public class InternalApiConfig extends Application {

    public static final String API_URI ="/internal";

    public InternalApiConfig() {
        // CDI
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(HealtCheckRest.class, PrometheusRestService.class, UttakRest.class);
    }

}
