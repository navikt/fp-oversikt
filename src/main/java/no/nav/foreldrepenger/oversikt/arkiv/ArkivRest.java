package no.nav.foreldrepenger.oversikt.arkiv;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/arkiv")
@ApplicationScoped
public class ArkivRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArkivRest.class);

    private ArkivTjeneste arkivTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;

    @Inject
    public ArkivRest(ArkivTjeneste arkivTjeneste, TilgangKontrollTjeneste tilgangkontroll, InnloggetBruker innloggetBruker) {
        this.arkivTjeneste = arkivTjeneste;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
    }

    ArkivRest() {
        // CDI
    }

    @GET
    @Path("/alle")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ArkivDokumentDto> alleArkiverteDokumenterPåSak(@QueryParam("saksnummer") @Valid @NotNull Saksnummer saksnummer) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        tilgangkontroll.sakKobletTilAktørGuard(saksnummer);
        var alle = arkivTjeneste.alle(innloggetBruker.fødselsnummer(), saksnummer);
        LOG.info("Hentet {} dokumenter på sak {}", alle.size(), saksnummer.value());
        return alle;

    }
}
