package no.nav.foreldrepenger.oversikt.server.konfig.swagger;

import java.util.Set;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import no.nav.openapi.spec.utils.openapi.DiscriminatorModelConverter;
import no.nav.openapi.spec.utils.openapi.EnumVarnamesConverter;
import no.nav.openapi.spec.utils.openapi.JsonSubTypesModelConverter;
import no.nav.openapi.spec.utils.openapi.NoJsonSubTypesAnnotationIntrospector;
import no.nav.openapi.spec.utils.openapi.RefToClassLookup;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

/**
 * Custom Reader for api som går ut mot typegenereing. Utenom vanlig reader så gjør denne:
 *  1) Her fjerner vi path (vi ønsker ikke mulighet til å kalle endepunktene via swagger)
 *  2) Registere ModulConverter med egendefinert Objectmapper i henhold til hva frontend forventer
 */
public class TypegenereringFrontendOpenApiReader extends Reader {

    @Override
    public OpenAPI read(Set<Class<?>> resourceClasses) {
        lagCustomModelConvertersForFrontendTypegenerering();
        var openApi = super.read(resourceClasses);
        openApi.setPaths(new Paths());
        return openApi;
    }

    private static void lagCustomModelConvertersForFrontendTypegenerering() {
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
    }

    private static ObjectMapper lagObjectMapperUtenJsonSubTypeAnnotasjoner() {
        final var om = DefaultJsonMapper.getJsonMapper().rebuild()
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

}
