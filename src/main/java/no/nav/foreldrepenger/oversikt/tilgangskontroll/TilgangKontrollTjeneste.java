package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

@ApplicationScoped
public class TilgangKontrollTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilgangKontrollTjeneste.class);

    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;
    // Bare fpsoknad skal lagre søknadsplan. AZURE_APP_PRE_AUTHORIZED_APPS brukes ikke, fordi den speiler hele
    // accessPolicy.inbound, også frontendene som bare skal bruke sluttbrukerendepunktene.
    private String tillattSystemklient;
    private boolean godtaAlleSystemklienter;

    @Inject
    public TilgangKontrollTjeneste(SakRepository sakRepository, InnloggetBruker innloggetBruker) {
        this(sakRepository, innloggetBruker, tillattSystemklient(Environment.current()), erVtp());
    }

    TilgangKontrollTjeneste(SakRepository sakRepository,
                            InnloggetBruker innloggetBruker,
                            String tillattSystemklient,
                            boolean godtaAlleSystemklienter) {
        this.sakRepository = sakRepository;
        this.innloggetBruker = innloggetBruker;
        this.tillattSystemklient = tillattSystemklient;
        this.godtaAlleSystemklienter = godtaAlleSystemklienter;
    }

    // Samme format som azp_name i Azure-token: <cluster>:<namespace>:<app>
    private static String tillattSystemklient(Environment env) {
        return env.clusterName() + ":" + env.namespace() + ":fpsoknad";
    }

    // VTP utsteder azp_name vtp:teamforeldrepenger:vtp for alle klienter, så allowlisten kan ikke brukes der.
    // Leser miljøvariabelen direkte: Environment.getProperty faller tilbake til application-vtp.properties og
    // Cluster.current() faller tilbake til VTP når variabelen mangler. Begge ville åpnet unntaket i dev og prod.
    private static boolean erVtp() {
        return "vtp".equals(System.getenv("NAIS_CLUSTER_NAME"));
    }

    TilgangKontrollTjeneste() {
        // CDI
    }

    public void tilgangssjekkMyndighetsalder() {
        if (!innloggetBruker.erMyndig()) {
            throw new OversiktManglerTilgangException(LokalFeilKode.IKKE_TILGANG_UMYNDIG);
        }
    }

    public void sakKobletTilAktørGuard(Saksnummer saksnummer) {
        var aktørId = innloggetBruker.aktørId();
        if (!sakRepository.erSakKobletTilAktør(saksnummer, aktørId)) {
            var alleSaksnummer = sakRepository.hentFor(aktørId).stream().map(s -> s.saksnummer().value()).toList();
            LOG.info("Saksnummer {} ikke koblet til bruker. Brukers saksnummer {}", saksnummer.value(), alleSaksnummer);
            throw new OversiktManglerTilgangException(LokalFeilKode.IKKE_TILGANG);
        }
    }

    public void sjekkAtKallErFraSystem() {
        var kontekst = KontekstHolder.getKontekst();
        if (!(kontekst instanceof RequestKontekst request) || !IdentType.Systemressurs.equals(request.getIdentType())) {
            throw new OversiktManglerTilgangException(LokalFeilKode.IKKE_TILGANG_IKKE_SYSTEM);
        }
        if (!erPreautorisert(request)) {
            throw new OversiktManglerTilgangException(LokalFeilKode.IKKE_TILGANG);
        }
    }

    private boolean erPreautorisert(RequestKontekst kontekst) {
        if (godtaAlleSystemklienter) {
            return true;
        }
        var klient = kontekst.getUid();
        if (tillattSystemklient.equals(klient)) {
            return true;
        }
        LOG.warn("Systemklient {} er ikke preautorisert for å lagre søknadsplan", klient);
        return false;
    }

    public void sjekkAtKallErFraBorger() {
        var kontekst = KontekstHolder.getKontekst();
        if (erBorger(kontekst)) {
            return;
        }
        throw new OversiktManglerTilgangException(LokalFeilKode.IKKE_TILGANG_IKKE_EKSTERN);
    }

    private boolean erBorger(Kontekst kontekst) {
        if (kontekst == null) {
            return false;
        }
        return IdentType.EksternBruker.equals(kontekst.getIdentType());
    }
}
