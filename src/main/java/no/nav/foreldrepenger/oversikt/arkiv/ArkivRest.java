package no.nav.foreldrepenger.oversikt.arkiv;

import static no.nav.foreldrepenger.common.domain.validation.InputValideringRegex.BARE_TALL;
import static no.nav.foreldrepenger.oversikt.saker.TilgangsstyringBorger.sjekkAtKallErFraBorger;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

@Path("/arkiv")
@ApplicationScoped
public class ArkivRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArkivRest.class);

    private ArkivTjeneste arkivTjeneste;
    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;

    @Inject
    public ArkivRest(ArkivTjeneste arkivTjeneste, SakRepository sakRepository, InnloggetBruker innloggetBruker) {
        this.arkivTjeneste = arkivTjeneste;
        this.sakRepository = sakRepository;
        this.innloggetBruker = innloggetBruker;
    }

    ArkivRest() {
        // CDI
    }

    @GET
    @Path("/alle")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ArkivDokumentDto> alleArkiverteDokumenterPåSak(@QueryParam("saksnummer") @NotNull @Pattern(regexp = BARE_TALL) String saksnummer) {
        sjekkAtKallErFraBorger();
        tilgangssjekkMyndighetsalder();
        sakKobletTilAktørGuard(saksnummer);
        var alle = arkivTjeneste.alle(innloggetBruker.fødselsnummer(), new Saksnummer(saksnummer));
        LOG.info("Hentet {} dokumenter på sak {}", alle.size(), saksnummer);
        return alle;

    }


    private void tilgangssjekkMyndighetsalder() {
        if (!innloggetBruker.erMyndig()) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_UMYNDIG);
        }
    }

    private void sakKobletTilAktørGuard(String saksnummer) {
        var aktørId = innloggetBruker.aktørId();
        if (!sakRepository.erSakKobletTilAktør(new Saksnummer(saksnummer), aktørId)) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG);
        }
    }
}
