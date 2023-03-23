package no.nav.foreldrepenger.oversikt.server;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("internal")
public class InternalApiConfig extends Application {

    public InternalApiConfig() {
        // CDI
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(HealtCheckRest.class);
    }

}
