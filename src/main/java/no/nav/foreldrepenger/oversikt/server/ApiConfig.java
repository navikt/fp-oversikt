package no.nav.foreldrepenger.oversikt.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import no.nav.security.token.support.jaxrs.JwtTokenContainerRequestFilter;

import org.glassfish.jersey.server.ServerProperties;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends Application {

    public static final String API_URI = "/api";


    @Override
    public Set<Class<?>> getClasses() {
        // eksponert grensesnitt bak sikkerhet
        return Set.of(
            JwtTokenContainerRequestFilter.class,
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
