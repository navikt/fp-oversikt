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
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/saker")
@ApplicationScoped
@Transactional
public class SakerRest {

    private static final Logger LOG = LoggerFactory.getLogger(SakerRest.class);
    private Saker saker;
    private InnloggetBruker innloggetBruker;
    private TilgangKontrollTjeneste tilgangkontroll;

    @Inject
    public SakerRest(Saker saker, InnloggetBruker innloggetBruker, TilgangKontrollTjeneste tilgangkontroll) {
        this.saker = saker;
        this.innloggetBruker = innloggetBruker;
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
        var aktørId = innloggetBruker.aktørId();
        return saker.hent(aktørId);
    }
}
