package no.nav.foreldrepenger.oversikt.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.glassfish.jersey.server.ServerProperties;

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
import no.nav.foreldrepenger.oversikt.arkiv.DokumentRest;
import no.nav.foreldrepenger.oversikt.drift.ManuellOppdateringAvSakDriftTjeneste;
import no.nav.foreldrepenger.oversikt.drift.ProsessTaskRestTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.InntektsmeldingRest;
import no.nav.foreldrepenger.oversikt.oppgave.OppgaveRest;
import no.nav.foreldrepenger.oversikt.oppslag.OppslagRest;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartRest;
import no.nav.foreldrepenger.oversikt.saker.SakerRest;
import no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeRest;
import no.nav.foreldrepenger.oversikt.uttak.UttakRest;
import no.nav.vedtak.exception.TekniskException;

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
        var resourceClassesToInclude = Stream.of(getImplementationClasses(), getForvaltningClasses(), Set.of(UttakRest.class))
            .flatMap(Set::stream)
            .map(Class::getName)
            .collect(Collectors.toSet());
        var oasConfig = new SwaggerConfiguration()
            .openAPI(oas)
            .prettyPrint(true)
            .resourceClasses(resourceClassesToInclude);

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
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(getImplementationClasses());
        classes.addAll(getForvaltningClasses());
        classes.addAll(getFellesConfigClasses());
        return classes;
    }

    private Set<Class<?>> getFellesConfigClasses() {
        return Set.of(AuthenticationFilter.class, GeneralRestExceptionMapper.class, JacksonJsonConfig.class);
    }

    private Set<Class<?>> getForvaltningClasses() {
        return Set.of(OpenApiResource.class, ProsessTaskRestTjeneste.class, ManuellOppdateringAvSakDriftTjeneste.class);
    }

    private Set<Class<?>> getImplementationClasses() {
        return Set.of(DokumentRest.class,
            TidslinjeRest.class,
            OppslagRest.class,
            InntektsmeldingRest.class,
            OppgaveRest.class,
            SakerRest.class,
            AnnenPartRest.class,
            ArbeidRest.class);
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
