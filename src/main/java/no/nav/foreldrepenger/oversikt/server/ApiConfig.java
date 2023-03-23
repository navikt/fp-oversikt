package no.nav.foreldrepenger.oversikt.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ServerProperties;

@ApplicationPath("/api")
public class ApiConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        // eksponert grensesnitt bak sikkerhet
        return Set.of(
            BeskyttetRest.class);
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}
