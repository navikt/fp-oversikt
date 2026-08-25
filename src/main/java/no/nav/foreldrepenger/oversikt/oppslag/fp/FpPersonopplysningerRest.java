package no.nav.foreldrepenger.oversikt.oppslag.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/personopplysninger/foreldrepenger")
@ApplicationScoped
@Transactional
public class FpPersonopplysningerRest {

    private FpPersonopplysningerDtoTjeneste dtoTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;

    FpPersonopplysningerRest() {
        // CDI
    }

    @Inject
    public FpPersonopplysningerRest(FpPersonopplysningerDtoTjeneste dtoTjeneste, TilgangKontrollTjeneste tilgangkontroll) {
        this.dtoTjeneste = dtoTjeneste;
        this.tilgangkontroll = tilgangkontroll;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public FpPersonopplysningerDto personopplysninger() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        return dtoTjeneste.forInnloggetPerson();
    }
}

