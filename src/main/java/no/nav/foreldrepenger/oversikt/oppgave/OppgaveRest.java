package no.nav.foreldrepenger.oversikt.oppgave;

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
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeId;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/oppgaver")
@ApplicationScoped
@Transactional
public class OppgaveRest {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveRest.class);
    private Oppgaver oppgaver;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;

    @Inject
    public OppgaveRest(Oppgaver oppgaver, TilgangKontrollTjeneste tilgangkontroll, InnloggetBruker innloggetBruker) {
        this.oppgaver = oppgaver;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
    }

    OppgaveRest() {
        // CDI
    }

    @GET
    @Path("/manglendevedlegg")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DokumentTypeId> manglendeVedlegg(@QueryParam("saksnummer") @Valid @NotNull Saksnummer saksnummer) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        tilgangkontroll.sakKobletTilAktørGuard(saksnummer);
        LOG.info("Henter manglede vedlegg for sak {}", saksnummer.value());
        return oppgaver.manglendeVedlegg(saksnummer);
    }

    @GET
    @Path("/tilbakekrevingsuttalelse")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<TilbakekrevingsInnslag> tilbakekrevingsuttalelser() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        LOG.info("Henter alle uttalelser om tilbakekreving på person");
        return oppgaver.tilbakekrevingsuttalelser(innloggetBruker.aktørId());
    }
}
