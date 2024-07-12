package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.pdlpip.TilgangPersondata;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.pdlpip.TilgangPersondataDto;
import no.nav.vedtak.sikkerhet.kontekst.Groups;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

@RequestScoped
public class TilgangKontrollTjeneste implements TilgangKontroll {

    private final Map<String, TilgangPersondataDto> tilgangPersondataDtoMap = new HashMap<>();
    private final Kontekst kontekst = KontekstHolder.getKontekst();

    private SakRepository sakRepository;
    private TilgangPersondata tilgangPersondata;
    private InnloggetBruker innloggetBruker;


    @Inject
    public TilgangKontrollTjeneste(SakRepository sakRepository, TilgangPersondata tilgangPersondata, InnloggetBruker innloggetBruker) {
        this.sakRepository = sakRepository;
        this.tilgangPersondata = tilgangPersondata;
        this.innloggetBruker = innloggetBruker;
    }

    TilgangKontrollTjeneste() {
        // CDI
    }

    private TilgangPersondataDto tilgangpersondata(String ident) {
        return tilgangPersondataDtoMap.computeIfAbsent(ident, verdi -> tilgangPersondata.hentTilgangPersondataBolk(List.of(verdi)).get(verdi));
    }

    @Override
    public void tilgangssjekkMyndighetsalder() {
        var fødselsnummer = innloggetBruker.fødselsnummer();
        var tilgangpersondata = tilgangpersondata(fødselsnummer.value());
        if (tilgangpersondata == null || tilgangpersondata.erIkkeMyndig()) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_UMYNDIG);
        }
    }

    @Override
    public boolean erUkjentPerson(String ident) {
        return tilgangpersondata(ident) == null;
    }

    @Override
    public boolean harAdresseBeskyttelse(String ident) {
        var tilgangpersondata = tilgangpersondata(ident);
        return tilgangpersondata == null || tilgangpersondata.harAdresseBeskyttelse();
    }

    @Override
    public void sakKobletTilAktørGuard(Saksnummer saksnummer) {
        var fødselsnummer = innloggetBruker.fødselsnummer();
        var tilgangpersondata = tilgangpersondata(fødselsnummer.value());
        if (tilgangpersondata == null || tilgangpersondata.aktoerId() == null || !sakRepository.erSakKobletTilAktør(saksnummer, new AktørId(tilgangpersondata.aktoerId()))) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG);
        }
    }

    @Override
    public void sjekkAtKallErFraBorger() {
        if (erBorger(kontekst)) {
            return;
        }
        throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_IKKE_EKSTERN);
    }


    @Override
    public void sjekkAtSaksbehandlerHarRollenDrift() {
        if (erSaksbehandler(kontekst) && saksbehandlerHarRollenDrift(kontekst)) {
            return;
        }
        throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_MANGLER_DRIFT_ROLLE);
    }

    @Override
    public Fødselsnummer fødselsnummer(AktørId aktørId) {
        return new Fødselsnummer(tilgangpersondata(aktørId.value()).personIdent());
    }

    private static boolean erSaksbehandler(Kontekst kontekst) {
        if (kontekst == null) {
            return false;
        }
        return IdentType.InternBruker.equals(kontekst.getIdentType());
    }

    private static boolean saksbehandlerHarRollenDrift(Kontekst kontekst) {
        if (kontekst == null) {
            return false;
        }
        return kontekst instanceof RequestKontekst requestKontekst && requestKontekst.harGruppe(Groups.DRIFT);
    }

    private static boolean erBorger(Kontekst kontekst) {
        if (kontekst == null) {
            return false;
        }
        return IdentType.EksternBruker.equals(kontekst.getIdentType());
    }
}
