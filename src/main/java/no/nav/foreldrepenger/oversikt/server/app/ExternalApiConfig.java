package no.nav.foreldrepenger.oversikt.server.app;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import no.nav.foreldrepenger.oversikt.uttak.UttakRest;

@ApplicationPath(ExternalApiConfig.API_URI)
public class ExternalApiConfig extends Application {

    public static final String API_URI ="/fpoversikt/external";

    public ExternalApiConfig() {
        // CDI
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(UttakRest.class);
    }

}
