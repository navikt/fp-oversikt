package no.nav.foreldrepenger.oversikt.server;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.arbeid.ArbeidRest;
import no.nav.foreldrepenger.oversikt.drift.ManuellOppdateringAvSakDriftTjeneste;
import no.nav.foreldrepenger.oversikt.drift.ProsessTaskRestTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.InntektsmeldingRest;
import no.nav.foreldrepenger.oversikt.oppgave.OppgaveRest;
import no.nav.foreldrepenger.oversikt.oppslag.OppslagRest;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartRest;
import no.nav.foreldrepenger.oversikt.saker.SakerRest;
import no.nav.vedtak.exception.TekniskException;
import org.glassfish.jersey.server.ServerProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends Application {

    public static final String API_URI ="/api";

    private static final Environment ENV = Environment.current();

    public ApiConfig() {
        var oas = new OpenAPI();
        var info = new Info()
            .title("FPOVERSIKT - saksoversikt")
            .version(Optional.ofNullable(ENV.imageName()).orElse("1.0"))
            .description("REST grensesnitt for FPOVERSIKT.");

        oas.info(info).addServersItem(new Server().url("/"));
        var oasConfig = new SwaggerConfiguration()
            .openAPI(oas)
            .prettyPrint(true)
            .resourceClasses(Set.of(ProsessTaskRestTjeneste.class.getName(), ManuellOppdateringAvSakDriftTjeneste.class.getName()));

        try {
            new GenericOpenApiContextBuilder<>()
                .openApiConfiguration(oasConfig)
                .buildContext(true)
                .read();
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        // eksponert grensesnitt bak sikkerhet. Nå er vi på max Set.of før varargs-versjonen.
        return Set.of(OppslagRest.class, InntektsmeldingRest.class, OppgaveRest.class, SakerRest.class, AnnenPartRest.class, ArbeidRest.class,
            ProsessTaskRestTjeneste.class, ManuellOppdateringAvSakDriftTjeneste.class,
            AuthenticationFilter.class, OpenApiResource.class, GeneralRestExceptionMapper.class);
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
