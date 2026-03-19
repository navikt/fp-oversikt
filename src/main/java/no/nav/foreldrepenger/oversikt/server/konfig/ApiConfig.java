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
import no.nav.foreldrepenger.oversikt.server.error.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.oversikt.server.error.ValidationExceptionMapper;
import no.nav.foreldrepenger.oversikt.server.konfig.swagger.OpenApiUtils;
import no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeRest;
import no.nav.vedtak.server.rest.AuthenticationFilter;
import no.nav.vedtak.server.rest.jackson.Jackson2MapperFeature;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends ResourceConfig {

    public static final String API_URI = "/api";
    private static final Environment ENV = Environment.current();

    public ApiConfig() {
        register(Jackson2MapperFeature.class); // Standard Jersey Jackson2 konfigurasjon
        register(AuthenticationFilter.class); // Autentisering
        registerClasses(getFellesConfigClasses());
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

    static Set<Class<?>> getFellesConfigClasses() {
        return  Set.of(
            GeneralRestExceptionMapper.class, // Exception handling
            ValidationExceptionMapper.class // Exception handling
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
        OpenApiUtils.openApiConfigFor("Fpoversikt - specifikasjon for typegenerering frontend", this)
            .readerClassTypegenereingFrontend()
            .registerClasses(getApplicationClasses())
            .buildOpenApiContext();
        register(OpenApiResource.class);
    }
}
