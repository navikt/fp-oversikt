package no.nav.foreldrepenger.oversikt.innhenting.tidslinje;

import java.util.List;

import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;

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
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;


@Path("/tidslinje")
@ApplicationScoped
public class TidslinjeRest {
    private static final Logger LOG = LoggerFactory.getLogger(TidslinjeRest.class);
    private TidslinjeTjeneste tidslinjeTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;

    @Inject
    public TidslinjeRest(TidslinjeTjeneste tidslinjeTjeneste, TilgangKontrollTjeneste tilgangkontroll, InnloggetBruker innloggetBruker) {
        this.tidslinjeTjeneste = tidslinjeTjeneste;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
    }

    public TidslinjeRest() {
        // CDI
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TidslinjeHendelseDto> tidslinje(@QueryParam("saksnummer") @Valid @NotNull Saksnummer saksnummer) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        tilgangkontroll.sakKobletTilAktørGuard(saksnummer);
        LOG.info("Henter tidslinje sak {}", saksnummer.value());
        return tidslinjeTjeneste.tidslinje(innloggetBruker.fødselsnummer(), saksnummer);
    }
}
