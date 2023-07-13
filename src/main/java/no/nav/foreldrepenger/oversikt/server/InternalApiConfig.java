package no.nav.foreldrepenger.oversikt.server;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("internal")
public class InternalApiConfig extends Application {

    public InternalApiConfig() {
        // CDI
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(HealtCheckRest.class, PrometheusRestService.class);
    }

}
