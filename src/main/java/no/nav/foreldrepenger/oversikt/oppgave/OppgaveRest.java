package no.nav.foreldrepenger.oversikt.oppgave;

import static no.nav.foreldrepenger.common.domain.validation.InputValideringRegex.BARE_TALL;
import static no.nav.foreldrepenger.oversikt.saker.TilgangsstyringBorger.sjekkAtKallErFraBorger;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

@Path("/oppgaver")
@ApplicationScoped
@Transactional
public class OppgaveRest {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveRest.class);
    private Oppgaver oppgaver;
    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;

    @Inject
    public OppgaveRest(Oppgaver oppgaver, SakRepository sakRepository, InnloggetBruker innloggetBruker) {
        this.oppgaver = oppgaver;
        this.sakRepository = sakRepository;
        this.innloggetBruker = innloggetBruker;
    }

    OppgaveRest() {
        // CDI
    }

    @GET
    @Path("/manglendevedlegg")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DokumentType> manglendeVedlegg(@NotNull @QueryParam("saksnummer") @Pattern(regexp = BARE_TALL) String saksnummer) {
        sjekkAtKallErFraBorger();
        tilgangssjekkMyndighetsalder();
        sakKobletTilAktørGuard(saksnummer);
        LOG.info("Henter manglede vedlegg for sak {}", saksnummer);
        return oppgaver.manglendeVedlegg(new Saksnummer(saksnummer));
    }

    @GET
    @Path("/tilbakekrevingsuttalelse")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<TilbakekrevingsInnslag> tilbakekrevingsuttalelser() {
        sjekkAtKallErFraBorger();
        tilgangssjekkMyndighetsalder();
        LOG.info("Henter alle uttalelser om tilbakekreving på person");
        return oppgaver.tilbakekrevingsuttalelser(innloggetBruker.aktørId());
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
