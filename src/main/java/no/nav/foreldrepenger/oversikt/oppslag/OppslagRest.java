package no.nav.foreldrepenger.oversikt.oppslag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.oppslag.dto.PersonDto;
import no.nav.foreldrepenger.oversikt.oppslag.dto.PersonMedArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/person")
@ApplicationScoped
@Transactional
public class OppslagRest {

    private OppslagTjeneste oppslagTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;

    public OppslagRest() {
        // CDI
    }

    @Inject
    public OppslagRest(OppslagTjeneste oppslagTjeneste, TilgangKontrollTjeneste tilgangkontroll) {
        this.oppslagTjeneste = oppslagTjeneste;
        this.tilgangkontroll = tilgangkontroll;
    }

    @Path("/info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PersonDto personinfo() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        return oppslagTjeneste.personinfoFor();
    }

    @Path("/info-med-arbeidsforhold")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PersonMedArbeidsforholdDto søkerinfo() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        return oppslagTjeneste.personinfoMedArbeidsforholdFor();
    }

}