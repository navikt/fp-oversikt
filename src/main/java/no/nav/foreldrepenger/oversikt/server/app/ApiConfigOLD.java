package no.nav.foreldrepenger.oversikt.server.app;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
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

@ApplicationPath(ApiConfigOLD.API_URI)
public class ApiConfigOLD extends Application {

    public static final String API_URI ="/api";

    private static final Environment ENV = Environment.current();

    public ApiConfigOLD() {
        // CDI
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
