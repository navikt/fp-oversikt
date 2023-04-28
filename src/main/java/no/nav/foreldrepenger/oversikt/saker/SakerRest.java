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

import no.nav.foreldrepenger.common.innsyn.Saker;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@Path("/saker")
@ApplicationScoped
@Transactional
public class SakerRest {

    private static final Logger LOG = LoggerFactory.getLogger(SakerRest.class);
    private FpSaker fpSaker;
    private PdlKlient pdlKlient;

    @Inject
    public SakerRest(FpSaker fpSaker, PdlKlient pdlKlient) {
        this.fpSaker = fpSaker;
        this.pdlKlient = pdlKlient;
    }

    SakerRest() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Saker hent() {
        LOG.info("Kall mot saker endepunkt");
        var aktørId = aktørIdForBruker();
        return fpSaker.hent(aktørId);
    }

    private String aktørIdForBruker() {
        var fnr = KontekstHolder.getKontekst().getUid();
        var aktørId = pdlKlient.hentAktørIdForPersonIdent(fnr).orElseThrow();
        LOG.info("Mapper fnr til aktørId");
        return aktørId;
    }
}
