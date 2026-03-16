package no.nav.foreldrepenger.oversikt.oppslag.oversikt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/personopplysninger/oversikt")
@ApplicationScoped
@Transactional
public class OversiktPersonopplysningerRest {

    private OversiktPersonopplysningerDtoTjeneste dtoTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;

    OversiktPersonopplysningerRest() {
        // CDI
    }

    @Inject
    public OversiktPersonopplysningerRest(OversiktPersonopplysningerDtoTjeneste dtoTjeneste,
                                          TilgangKontrollTjeneste tilgangkontroll) {
        this.dtoTjeneste = dtoTjeneste;
        this.tilgangkontroll = tilgangkontroll;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OversiktPersonopplysningerDto personopplysninger() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        return dtoTjeneste.forInnloggetPerson();
    }

}
