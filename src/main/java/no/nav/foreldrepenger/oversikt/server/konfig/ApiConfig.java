package no.nav.foreldrepenger.oversikt.server.konfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.ApplicationPath;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.arbeid.ArbeidRest;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentRest;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.InntektsmeldingRest;
import no.nav.foreldrepenger.oversikt.oppgave.OppgaveRest;
import no.nav.foreldrepenger.oversikt.oppslag.es.EsPersonopplysningerRest;
import no.nav.foreldrepenger.oversikt.oppslag.fp.FpPersonopplysningerRest;
import no.nav.foreldrepenger.oversikt.oppslag.oversikt.OversiktPersonopplysningerRest;
import no.nav.foreldrepenger.oversikt.oppslag.svp.SvpPersonopplysningerRest;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartRest;
import no.nav.foreldrepenger.oversikt.saker.SakerRest;
import no.nav.foreldrepenger.oversikt.server.error.LokalValidationExceptionMapper;
import no.nav.foreldrepenger.oversikt.server.konfig.swagger.TypegenereringFrontendOpenApiReader;
import no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeRest;
import no.nav.vedtak.openapi.OpenApiUtils;
import no.nav.vedtak.server.rest.AuthenticationFilter;
import no.nav.vedtak.server.rest.GeneralRestExceptionMapper;
import no.nav.vedtak.server.rest.jackson.Jackson2MapperFeature;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends ResourceConfig {

    public static final String API_URI = "/api";
    private static final Environment ENV = Environment.current();

    public ApiConfig() {
        // Nesten standard FpRestJackson2-oppsett, men lokale tilpasninger av exceptions.
        register(Jackson2MapperFeature.class); // Standard Jersey Jackson2 konfigurasjon
        register(AuthenticationFilter.class); // Felles Autentisering
        register(GeneralRestExceptionMapper.class); // Felles Exception handling
        register(LokalValidationExceptionMapper.class); // Lokal exception handling med utvidet logging
        if (!ENV.isProd()) {
            registerOpenApi();
        }
        registerClasses(getApplicationClasses());
        setProperties(getApplicationProperties());
    }

    private static Set<Class<?>> getApplicationClasses() {
        return Set.of(
            DokumentRest.class,
            TidslinjeRest.class,
            EsPersonopplysningerRest.class,
            FpPersonopplysningerRest.class,
            SvpPersonopplysningerRest.class,
            OversiktPersonopplysningerRest.class,
            InntektsmeldingRest.class,
            OppgaveRest.class,
            SakerRest.class,
            AnnenPartRest.class,
            ArbeidRest.class
        );
    }


    static Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

    private void registerOpenApi() {
        var contextPath = ENV.getProperty("context.path", "/fpoversikt");
        OpenApiUtils.openApiConfigFor("FPOVERSIKT Forvaltning - saksoversikt", contextPath,this)
            .readerClass(TypegenereringFrontendOpenApiReader.class)
            .registerClasses(getApplicationClasses())
            .buildOpenApiContext();
        register(OpenApiResource.class);
    }
}
