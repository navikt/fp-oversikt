package no.nav.foreldrepenger.oversikt.server.konfig;

import static no.nav.foreldrepenger.oversikt.server.konfig.ApiConfig.getApplicationProperties;
import static no.nav.foreldrepenger.oversikt.server.konfig.ApiConfig.getFellesConfigClasses;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.ApplicationPath;
import no.nav.foreldrepenger.oversikt.drift.ManuellOppdateringAvSakDriftTjeneste;
import no.nav.foreldrepenger.oversikt.server.konfig.swagger.OpenApiUtils;
import no.nav.foreldrepenger.oversikt.server.sikkerhet.ForvaltningAuthorizationFilter;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;

@ApplicationPath(ForvaltningApiConfig.API_URI)
public class ForvaltningApiConfig extends ResourceConfig {
    public static final String API_URI = "/forvaltning/api";

    public ForvaltningApiConfig() {
        register(ForvaltningAuthorizationFilter.class); // Autorisering - drift
        registerClasses(getFellesConfigClasses());
        registerOpenApi();
        registerClasses(getForvaltningKlasser());
        setProperties(getApplicationProperties());
    }

    private static Set<Class<?>> getForvaltningKlasser() {
        return Set.of(
            ProsessTaskRestTjeneste.class,
            ManuellOppdateringAvSakDriftTjeneste.class
        );
    }

    private void registerOpenApi() {
        OpenApiUtils.openApiConfigFor("FPOVERSIKT - saksoversikt", this)
            .registerClasses(getForvaltningKlasser())
            .buildOpenApiContext();
        register(OpenApiResource.class);
    }
}
