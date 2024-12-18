package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class TilgangKontrollTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilgangKontrollTjeneste.class);

    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;

    @Inject
    public TilgangKontrollTjeneste(SakRepository sakRepository, InnloggetBruker innloggetBruker) {
        this.sakRepository = sakRepository;
        this.innloggetBruker = innloggetBruker;
    }

    TilgangKontrollTjeneste() {
        // CDI
    }

    public void tilgangssjekkMyndighetsalder() {
        if (!innloggetBruker.erMyndig()) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_UMYNDIG);
        }
    }

    public void sakKobletTilAktørGuard(Saksnummer saksnummer) {
        var aktørId = innloggetBruker.aktørId();
        if (!sakRepository.erSakKobletTilAktør(saksnummer, aktørId)) {
            var alleSaksnummer = sakRepository.hentFor(aktørId).stream().map(s -> s.saksnummer().value()).toList();
            LOG.info("Saksnummer {} ikke koblet til bruker. Brukers saksnummer {}", saksnummer.value(), alleSaksnummer);
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG);
        }
    }

    public void sjekkAtKallErFraBorger() {
        var kontekst = KontekstHolder.getKontekst();
        if (erBorger(kontekst)) {
            return;
        }
        throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_IKKE_EKSTERN);
    }

    private boolean erBorger(Kontekst kontekst) {
        if (kontekst == null) {
            return false;
        }
        return IdentType.EksternBruker.equals(kontekst.getIdentType());
    }
}
