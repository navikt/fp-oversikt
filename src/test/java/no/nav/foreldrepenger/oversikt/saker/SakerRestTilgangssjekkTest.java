package no.nav.foreldrepenger.oversikt.saker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Application;

import no.nav.foreldrepenger.oversikt.server.ProblemDetails;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.jupiter.api.Test;

public class SakerRestTilgangssjekkTest extends JerseyTest {

    private final static Saker saker = mock(Saker.class);
    private final static InnloggetBruker innloggetBruker = mock(InnloggetBruker.class);


    @Override
    public Application configure() {
        forceSet(TestProperties.CONTAINER_PORT, "0");
        return new ResourceConfig()
            .register(SakerRest.class)
            .register(new GeneralRestExceptionMapper())
            .register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(saker).to(Saker.class);
                    bind(innloggetBruker).to(InnloggetBruker.class);
                }
            });
    }

    @Test
    void myndig_innlogget_bruker_skal_gi_2xx() {
        when(innloggetBruker.erMyndig()).thenReturn(true);
        var response = target("/saker").request().get();
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    void umyndig_innlogget_bruker_skal_gi_403() {
        when(innloggetBruker.erMyndig()).thenReturn(false);
        var response = target("/saker").request().get();
        assertThat(response.getStatus()).isEqualTo(403);
        var detaljer = response.readEntity(ProblemDetails.class);
        assertThat(detaljer.message()).isEqualTo("Innlogget bruker er under myndighetsalder");
        assertThat(detaljer.error()).isEqualTo("UMYNDIG_BRUKER");
        assertThat(detaljer.status()).isEqualTo(403);
    }


}
