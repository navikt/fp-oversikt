package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.validator.JwtUtil;

@ApplicationScoped
public class TilgangKontrollTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilgangKontrollTjeneste.class);

    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;
    private Supplier<String> preautoriserteApper;

    @Inject
    public TilgangKontrollTjeneste(SakRepository sakRepository, InnloggetBruker innloggetBruker) {
        this(sakRepository, innloggetBruker, () -> Environment.current().getProperty("AZURE_APP_PRE_AUTHORIZED_APPS", ""));
    }

    TilgangKontrollTjeneste(SakRepository sakRepository, InnloggetBruker innloggetBruker, Supplier<String> preautoriserteApper) {
        this.sakRepository = sakRepository;
        this.innloggetBruker = innloggetBruker;
        this.preautoriserteApper = preautoriserteApper;
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
        // TODO Før produksjonssetting: avklar hvilke klienter som skal kunne lagre søknader,
        // og verifiser at AZURE_APP_PRE_AUTHORIZED_APPS og accessPolicy bare tillater disse.
        try {
            // Autentiseringsfilteret har validert tokenet før request-konteksten opprettes.
            var claims = JwtUtil.getClaims(kontekst.getToken().token());
            var klientId = UUID.fromString(JwtUtil.getStringClaim(claims, "azp"));
            var apper = DefaultJsonMapper.fromJson(preautoriserteApper.get(), PreautorisertApp[].class);
            var klienter = Arrays.stream(apper).map(app -> UUID.fromString(app.clientId())).toList();
            return klienter.contains(klientId);
        } catch (RuntimeException e) {
            // Parsefeil kan inneholde token eller konfigurasjon. Ikke viderefør eller logg årsaken.
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PreautorisertApp(String clientId) {
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
