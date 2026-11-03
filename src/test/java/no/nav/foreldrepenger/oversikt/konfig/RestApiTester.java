package no.nav.foreldrepenger.oversikt.konfig;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.Path;
import no.nav.foreldrepenger.oversikt.server.konfig.ApiConfig;
import no.nav.foreldrepenger.oversikt.server.konfig.BackendApiConfig;

class RestApiTester {

    static final List<Class<?>> UNNTATT = Collections.singletonList(OpenApiResource.class);

    static Collection<Method> finnAlleRestMetoder() {
        List<Method> liste = new ArrayList<>();
        for (Class<?> klasse : finnAlleRestTjenester()) {
            for (Method method : klasse.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    liste.add(method);
                }
            }
        }
        return liste;
    }


    static Collection<Class<?>> finnAlleRestTjenester() {
        return Stream.of(new ApiConfig(), new BackendApiConfig())
            .flatMap(config -> config.getClasses().stream())
            .filter(c -> c.getAnnotation(Path.class) != null)
            .filter(c -> !UNNTATT.contains(c))
            .collect(Collectors.toList());
    }
}
