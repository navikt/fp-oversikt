package no.nav.foreldrepenger.oversikt.innhenting.tidslinje;

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


@Path("/tidslinje")
@ApplicationScoped
public class TidslinjeRest {
    private static final Logger LOG = LoggerFactory.getLogger(TidslinjeRest.class);
    private TidslinjeTjeneste tidslinjeTjeneste;
    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;

    @Inject
    public TidslinjeRest(TidslinjeTjeneste tidslinjeTjeneste, SakRepository sakRepository, InnloggetBruker innloggetBruker) {
        this.tidslinjeTjeneste = tidslinjeTjeneste;
        this.sakRepository = sakRepository;
        this.innloggetBruker = innloggetBruker;
    }

    public TidslinjeRest() {
        // CDI
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TidslinjeHendelseDto> tidslinje(@NotNull @QueryParam("saksnummer") @Pattern(regexp = BARE_TALL) String saksnummer) {
        sjekkAtKallErFraBorger();
        tilgangssjekkMyndighetsalder();
        sakKobletTilAktørGuard(saksnummer);
        LOG.info("Henter tidslinje sak {}", saksnummer);
        return tidslinjeTjeneste.tidslinje(innloggetBruker.fødselsnummer(), new Saksnummer(saksnummer));
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
