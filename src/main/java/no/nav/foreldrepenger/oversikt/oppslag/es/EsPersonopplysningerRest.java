package no.nav.foreldrepenger.oversikt.oppslag.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/personopplysninger/engangsstonad")
@ApplicationScoped
@Transactional
public class EsPersonopplysningerRest {

    private EsPersonopplysningerDtoTjeneste dtoTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;

    EsPersonopplysningerRest() {
        // CDI
    }

    @Inject
    public EsPersonopplysningerRest(EsPersonopplysningerDtoTjeneste dtoTjeneste, TilgangKontrollTjeneste tilgangkontroll) {
        this.dtoTjeneste = dtoTjeneste;
        this.tilgangkontroll = tilgangkontroll;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EsPersonopplysningerDto personopplysninger() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        return dtoTjeneste.forInnloggetPerson();
    }
}

