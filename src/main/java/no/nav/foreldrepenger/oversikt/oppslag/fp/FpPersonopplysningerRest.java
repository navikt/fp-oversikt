package no.nav.foreldrepenger.oversikt.oppslag.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.integrasjoner.brreg.BrregRollerTjeneste;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/personopplysninger/foreldrepenger")
@ApplicationScoped
@Transactional
public class FpPersonopplysningerRest {

    private FpPersonopplysningerDtoTjeneste dtoTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;
    private BrregRollerTjeneste brregRollerTjeneste;

    FpPersonopplysningerRest() {
        // CDI
    }

    @Inject
    public FpPersonopplysningerRest(FpPersonopplysningerDtoTjeneste dtoTjeneste,
                                    TilgangKontrollTjeneste tilgangkontroll,
                                    InnloggetBruker innloggetBruker,
                                    BrregRollerTjeneste brregRollerTjeneste) {
        this.dtoTjeneste = dtoTjeneste;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
        this.brregRollerTjeneste = brregRollerTjeneste;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public FpPersonopplysningerDto personopplysninger() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        brregRollerTjeneste.testBrregIntegrasjonIProduksjon(innloggetBruker.fødselsnummer());
        return dtoTjeneste.forInnloggetPerson();
    }
}

