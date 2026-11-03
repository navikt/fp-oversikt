package no.nav.foreldrepenger.oversikt.server.konfig;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.ApplicationPath;
import no.nav.foreldrepenger.oversikt.soknadsplan.SøknadsplanRest;
import no.nav.vedtak.server.rest.AuthenticationFilter;
import no.nav.vedtak.server.rest.FpRestJacksonFeature;
import no.nav.vedtak.server.rest.RestSecureLogFeature;

/**
 * For kall fra andre backend-apper med systembruker (Azure client credentials).
 * Ressursene kontrollerer selv hvilke systemklienter som har tilgang.
 */
@ApplicationPath(BackendApiConfig.API_URI)
public class BackendApiConfig extends ResourceConfig {

    public static final String API_URI = "/backend/api";

    public BackendApiConfig() {
        register(AuthenticationFilter.class);
        register(FpRestJacksonFeature.class); // Standard Jersey Jackson konfigurasjon
        register(RestSecureLogFeature.class); // Logg feil i secure log
        registerClasses(getApplicationClasses());
        setProperties(ApiConfig.getApplicationProperties());
    }

    static Set<Class<?>> getApplicationClasses() {
        return Set.of(SøknadsplanRest.class);
    }
}
