package no.nav.foreldrepenger.oversikt.arkiv;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.common.domain.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
@Path("/dokument")
public class DokumentRest {

    private SafSelvbetjeningTjeneste safSelvbetjeningTjeneste;
    private InnloggetBruker innloggetBruker;
    private TilgangKontrollTjeneste tilgangkontroll;

    public DokumentRest() {
        // CDI
    }

    @Inject
    public DokumentRest(SafSelvbetjeningTjeneste safSelvbetjeningTjeneste,
                        InnloggetBruker innloggetBruker,
                        TilgangKontrollTjeneste tilgangkontroll) {
        this.safSelvbetjeningTjeneste = safSelvbetjeningTjeneste;
        this.innloggetBruker = innloggetBruker;
        this.tilgangkontroll = tilgangkontroll;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public byte[] dokument(@QueryParam("journalpostId") @Valid @NotNull JournalpostId journalpostId,
                           @QueryParam("dokumentId") @Valid @NotNull DokumentId dokumentId) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        return safSelvbetjeningTjeneste.hentDokument(journalpostId, dokumentId);
    }

    @GET
    @Path("/alle")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DokumentDto> alleDokumenterPåSak(@QueryParam("saksnummer") @Valid @NotNull Saksnummer saksnummer) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        var saksnummerDomene = new no.nav.foreldrepenger.oversikt.domene.Saksnummer(saksnummer.value());
        tilgangkontroll.sakKobletTilAktørGuard(saksnummerDomene);
        var alleJournalposter = safSelvbetjeningTjeneste.alleJournalposter(innloggetBruker.fødselsnummer(), saksnummerDomene);
        return tilArkivDokumenter(alleJournalposter);
    }

    private static List<DokumentDto> tilArkivDokumenter(List<EnkelJournalpostSelvbetjening> journalposter) {
        return journalposter.stream()
                .flatMap(enkelJournalpost -> enkelJournalpost.dokumenter().stream()
                        .map(dokument -> tilArkivdokument(dokument, enkelJournalpost))
                )
                .sorted(Comparator.comparing(DokumentDto::mottatt))
                .toList();
    }

    private static DokumentDto tilArkivdokument(EnkelJournalpostSelvbetjening.Dokument dokument, EnkelJournalpostSelvbetjening enkelJournalpost) {
        return new DokumentDto(
                dokument.tittel() != null ? dokument.tittel() : enkelJournalpost.tittel(),
                enkelJournalpost.type(),
                enkelJournalpost.saksnummer(),
                enkelJournalpost.journalpostId(),
                dokument.dokumentId(),
                enkelJournalpost.mottatt()
        );
    }
}
