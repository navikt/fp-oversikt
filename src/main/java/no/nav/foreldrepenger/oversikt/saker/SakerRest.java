package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.saker.TilgangsstyringBorger.sjekkAtKallErFraBorger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

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
        sjekkAtKallErFraBorger();
        tilgangssjekkMyndighetsalder();
        LOG.info("Kall mot saker endepunkt");
        var aktørId = innloggetBruker.aktørId();
        return saker.hent(aktørId);
    }

    private void tilgangssjekkMyndighetsalder() {
        if (!innloggetBruker.erMyndig()) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_UMYNDIG);
        }
    }
}
