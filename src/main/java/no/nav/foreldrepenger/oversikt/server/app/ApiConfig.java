package no.nav.foreldrepenger.oversikt.server.app;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.databind.json.JsonMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;

import io.swagger.v3.core.jackson.TypeNameResolver;

import io.swagger.v3.core.util.ObjectMapperFactory;

import no.nav.foreldrepenger.oversikt.uttak.UttakRest;
import no.nav.openapi.spec.utils.openapi.DiscriminatorModelConverter;
import no.nav.openapi.spec.utils.openapi.EnumVarnamesConverter;
import no.nav.openapi.spec.utils.openapi.JsonSubTypesModelConverter;
import no.nav.openapi.spec.utils.openapi.NoJsonSubTypesAnnotationIntrospector;

import no.nav.openapi.spec.utils.openapi.RefToClassLookup;

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
import no.nav.foreldrepenger.oversikt.server.AuthenticationFilter;
import no.nav.foreldrepenger.oversikt.server.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.oversikt.server.JacksonJsonConfig;
import no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeRest;
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

        oas.info(info).addServersItem(new Server().url(ENV.getProperty("context.path", "/fpoversikt")));
        var oasConfig = new SwaggerConfiguration()
            .openAPI(oas)
            .prettyPrint(true)
            .resourceClasses(Set.of(ProsessTaskRestTjeneste.class.getName(), ManuellOppdateringAvSakDriftTjeneste.class.getName(), UttakRest.class.getName()));

        try {
            // Påfølgende ModelConverts oppsett er tilpasset fra K9 sin openapi-spec-utils: https://github.com/navikt/openapi-spec-utils

            // Denne gjør at enums trekkes ut som egne typer istedenfor inline
            ModelResolver.enumsAsRef = true;
            ModelConverters.reset();
            var typeNameResolver = TypeNameResolver.std;
            typeNameResolver.setUseFqn(true);

            ModelConverters.getInstance().addConverter(new ModelResolver(lagObjectMapperUtenJsonSubTypeAnnotasjoner(),  typeNameResolver));
            ModelConverters.getInstance().addConverter(new JsonSubTypesModelConverter());
            ModelConverters.getInstance().addConverter(new DiscriminatorModelConverter(new RefToClassLookup()));
            ModelConverters.getInstance().addConverter(new EnumVarnamesConverter());

            var context = new GenericOpenApiContextBuilder<>()
                .openApiConfiguration(oasConfig)
                .buildContext(false);

            context.init();
            context.read();
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }
    }

    private static ObjectMapper lagObjectMapperUtenJsonSubTypeAnnotasjoner() {
        final var om = JsonMapper.builder(ObjectMapperFactory.createJson().getFactory())
            // OpenApi-spec som blir generert er ikke alltid konsekvent på rekkefølgen til properties.
            // Ved å skru på disse flaggene blir output deterministic og det blir enklere å se hva som faktisk er diff fra forrige typegenerering
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();
        // Fjern alle annotasjoner om JsonSubTypes. Hvis disse er med i generasjon av openapi spec får vi sirkulære avhengigheter.
        // Det skjer ved at superklassen sier den har "oneOf" arvingene sine. Mens en arving sier den har "allOf" forelderen sin.
        // Ved å fjerne jsonSubType annotasjoner får vi heller en enveis-lenke der superklassen definerer arvingene sine med "oneOf".
        om.setAnnotationIntrospector(new NoJsonSubTypesAnnotationIntrospector());
        return om;
    }

    @Override
    public Set<Class<?>> getClasses() {
        // eksponert grensesnitt bak sikkerhet. Nå er vi på max Set.of før varargs-versjonen.
        return Set.of(
            // Providers/filters/teknisk
            AuthenticationFilter.class,
            OpenApiResource.class,
            GeneralRestExceptionMapper.class,
            JacksonJsonConfig.class,

            // API
            DokumentRest.class,
            TidslinjeRest.class,
            OppslagRest.class,
            InntektsmeldingRest.class,
            OppgaveRest.class,
            SakerRest.class,
            AnnenPartRest.class,
            ArbeidRest.class,
            ProsessTaskRestTjeneste.class,
            ManuellOppdateringAvSakDriftTjeneste.class);
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
