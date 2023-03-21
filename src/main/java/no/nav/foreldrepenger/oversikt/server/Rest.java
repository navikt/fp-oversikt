package no.nav.foreldrepenger.oversikt.server;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("test")
@ApplicationScoped
public class Rest {

    private static final Logger LOG = LoggerFactory.getLogger(Rest.class);

    @GET
    public boolean test() {
        LOG.info("hei på deg");
        return true;
    }
    //http://localhost:8889/fpoversikt/internal/test
}
