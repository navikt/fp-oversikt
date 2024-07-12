package no.nav.foreldrepenger.oversikt.saker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontroll;

@Path("/saker")
@ApplicationScoped
@Transactional
public class SakerRest {

    private static final Logger LOG = LoggerFactory.getLogger(SakerRest.class);
    private Saker saker;
    private TilgangKontroll tilgangkontroll;

    @Inject
    public SakerRest(Saker saker, TilgangKontroll tilgangkontroll) {
        this.saker = saker;
        this.tilgangkontroll = tilgangkontroll;
    }

    SakerRest() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public no.nav.foreldrepenger.common.innsyn.Saker hent() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        LOG.debug("Kall mot saker endepunkt");
        return saker.hent();
    }
}
