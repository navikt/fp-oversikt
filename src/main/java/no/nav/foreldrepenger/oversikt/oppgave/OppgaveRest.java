package no.nav.foreldrepenger.oversikt.oppgave;

import static no.nav.foreldrepenger.oversikt.saker.TilgangsstyringBorger.sjekkAtKallErFraBorger;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    public List<DokumentType> manglendeVedlegg(@NotNull @QueryParam("saksnummer") @Valid Saksnummer saksnummer) {
        sjekkAtKallErFraBorger();
        tilgangssjekkMyndighetsalder();
        sakKobletTilAktørGuard(saksnummer);
        LOG.info("Henter manglede vedlegg for sak {}", saksnummer.value());
        return oppgaver.manglendeVedlegg(saksnummer);
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

    private void sakKobletTilAktørGuard(Saksnummer saksnummer) {
        var aktørId = innloggetBruker.aktørId();
        if (!sakRepository.erSakKobletTilAktør(saksnummer, aktørId)) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG);
        }
    }
}
