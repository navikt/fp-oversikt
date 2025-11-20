package no.nav.foreldrepenger.oversikt.server.konfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import no.nav.foreldrepenger.oversikt.innhenting.beregning.BeregningRest;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.ApplicationPath;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.arbeid.ArbeidRest;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentRest;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.InntektsmeldingRest;
import no.nav.foreldrepenger.oversikt.oppgave.OppgaveRest;
import no.nav.foreldrepenger.oversikt.oppslag.OppslagRest;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartRest;
import no.nav.foreldrepenger.oversikt.saker.SakerRest;
import no.nav.foreldrepenger.oversikt.server.JacksonJsonConfig;
import no.nav.foreldrepenger.oversikt.server.error.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.oversikt.server.error.ValidationExceptionMapper;
import no.nav.foreldrepenger.oversikt.server.konfig.swagger.OpenApiUtils;
import no.nav.foreldrepenger.oversikt.server.sikkerhet.AuthenticationFilter;
import no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeRest;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends ResourceConfig {

    public static final String API_URI = "/api";
    private static final Environment ENV = Environment.current();

    public ApiConfig() {
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
            OppslagRest.class,
            InntektsmeldingRest.class,
            BeregningRest.class,
            OppgaveRest.class,
            SakerRest.class,
            AnnenPartRest.class,
            ArbeidRest.class
        );
    }

    static Set<Class<?>> getFellesConfigClasses() {
        return  Set.of(
            AuthenticationFilter.class, // Autentisering
            GeneralRestExceptionMapper.class, // Exception handling
            ValidationExceptionMapper.class, // Exception handling
            JacksonJsonConfig.class // Json
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
