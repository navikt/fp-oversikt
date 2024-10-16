package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Path("/inntektsmeldinger")
@ApplicationScoped
public class InntektsmeldingRest {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingRest.class);

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;

    @Inject
    public InntektsmeldingRest(InntektsmeldingTjeneste inntektsmeldingTjeneste, TilgangKontrollTjeneste tilgangkontroll) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.tilgangkontroll = tilgangkontroll;
    }

    InntektsmeldingRest() {
        // CDI
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FpOversiktInntektsmeldingDto> alleInntektsmeldinger(@QueryParam("saksnummer") @Valid @NotNull Saksnummer saksnummer) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        var inntektsmeldinger = inntektsmeldingTjeneste.inntektsmeldinger(saksnummer);
        LOG.info("Hentet {} inntektsmeldinger på sak {}", inntektsmeldinger.size(), saksnummer.value());
        return inntektsmeldinger;
    }
}
