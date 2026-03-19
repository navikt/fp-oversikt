package no.nav.foreldrepenger.oversikt.server.konfig;

import static no.nav.foreldrepenger.oversikt.server.konfig.ApiConfig.getApplicationProperties;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.ApplicationPath;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.drift.ManuellOppdateringAvSakDriftTjeneste;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;
import no.nav.vedtak.openapi.OpenApiUtils;
import no.nav.vedtak.server.rest.ForvaltningAuthorizationFilter;
import no.nav.vedtak.server.rest.FpRestJackson2Feature;

@ApplicationPath(ForvaltningApiConfig.API_URI)
public class ForvaltningApiConfig extends ResourceConfig {
    public static final String API_URI = "/forvaltning/api";

    private static final Environment ENV = Environment.current();

    public ForvaltningApiConfig() {
        register(FpRestJackson2Feature.class); // Standard Jersey Jackson2 konfigurasjon
        register(ForvaltningAuthorizationFilter.class); // Autorisering - drift
        registerOpenApi();
        registerClasses(getForvaltningKlasser());
        setProperties(getApplicationProperties());
    }

    private static Set<Class<?>> getForvaltningKlasser() {
        return Set.of(ProsessTaskRestTjeneste.class, ManuellOppdateringAvSakDriftTjeneste.class);
    }

    private void registerOpenApi() {
        var contextPath = ENV.getProperty("context.path", "/fpoversikt");
        OpenApiUtils.setupOpenApi("FPOVERSIKT Forvaltning - saksoversikt",
                contextPath, getForvaltningKlasser(), this);
        register(OpenApiResource.class);
    }
}
