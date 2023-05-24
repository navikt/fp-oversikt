package no.nav.foreldrepenger.oversikt.saker;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/saker")
@ApplicationScoped
@Transactional
public class SakerRest {

    private static final Logger LOG = LoggerFactory.getLogger(SakerRest.class);
    private Saker saker;
    private InnloggetBruker innloggetBruker;

    @Inject
    public SakerRest(Saker saker, InnloggetBruker innloggetBruker) {
        this.saker = saker;
        this.innloggetBruker = innloggetBruker;
    }

    SakerRest() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public no.nav.foreldrepenger.common.innsyn.Saker hent() {
        LOG.info("Kall mot saker endepunkt");
        tilgangssjekkMyndighetsalder();
        var aktørId = innloggetBruker.aktørId();
        return saker.hent(aktørId);
    }

    private void tilgangssjekkMyndighetsalder() {
        if (!innloggetBruker.erMyndig()) {
            throw new GeneralRestExceptionMapper.UmyndigBrukerException();
        }
    }
}
