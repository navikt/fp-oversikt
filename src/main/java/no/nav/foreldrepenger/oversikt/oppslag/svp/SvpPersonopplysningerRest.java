package no.nav.foreldrepenger.oversikt.oppslag.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/personopplysninger/svangerskapspenger")
@ApplicationScoped
@Transactional
public class SvpPersonopplysningerRest {

    private SvpPersonopplysningerDtoTjeneste dtoTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;

    SvpPersonopplysningerRest() {
        // CDI
    }

    @Inject
    public SvpPersonopplysningerRest(SvpPersonopplysningerDtoTjeneste dtoTjeneste, TilgangKontrollTjeneste tilgangkontroll) {
        this.dtoTjeneste = dtoTjeneste;
        this.tilgangkontroll = tilgangkontroll;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SvpPersonopplysningerDto personopplysninger() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        return dtoTjeneste.forInnloggetPerson();
    }
}

