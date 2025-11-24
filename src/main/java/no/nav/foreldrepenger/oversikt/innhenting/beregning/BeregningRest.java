package no.nav.foreldrepenger.oversikt.innhenting.beregning;

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
import no.nav.foreldrepenger.oversikt.domene.beregning.Beregning;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Path("/beregning")
@ApplicationScoped
public class BeregningRest {
    private static final Logger LOG = LoggerFactory.getLogger(BeregningRest.class);

    private BeregningTjeneste beregningTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;

    @Inject
    public BeregningRest(BeregningTjeneste beregningTjeneste, TilgangKontrollTjeneste tilgangkontroll) {
        this.beregningTjeneste = beregningTjeneste;
        this.tilgangkontroll = tilgangkontroll;
    }

    BeregningRest() {
        // CDI
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // TODO: lag DTO type
    public Beregning beregning(@QueryParam("saksnummer") @Valid @NotNull Saksnummer saksnummer) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        tilgangkontroll.sakKobletTilAktørGuard(saksnummer);
        var beregning = beregningTjeneste.finnBeregning(saksnummer);
        LOG.info("Hentet beregning på sak {}", saksnummer.value());
        return beregning;
    }
}
