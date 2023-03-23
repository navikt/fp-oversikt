package no.nav.foreldrepenger.oversikt.server;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/test")
@ApplicationScoped
public class BeskyttetRest {

    private static final CacheControl CC = cacheControl();
    private static final Logger LOG = LoggerFactory.getLogger(BeskyttetRest.class);

    @GET
    public Response hello() {
        LOG.info("hei på deg");
        return Response.ok("ok").cacheControl(CC).build();
    }

    private static CacheControl cacheControl() {
        var cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMustRevalidate(true);
        return cc;
    }
}
