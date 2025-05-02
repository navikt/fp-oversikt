package no.nav.foreldrepenger.oversikt.arkiv;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.saf.Bruker;
import no.nav.saf.BrukerIdType;
import no.nav.saf.BrukerResponseProjection;
import no.nav.saf.DokumentInfoResponseProjection;
import no.nav.saf.Journalpost;
import no.nav.saf.JournalpostQueryRequest;
import no.nav.saf.JournalpostResponseProjection;
import no.nav.saf.Journalposttype;
import no.nav.saf.SakResponseProjection;
import no.nav.saf.Tilleggsopplysning;
import no.nav.saf.TilleggsopplysningResponseProjection;
import no.nav.vedtak.felles.integrasjon.saf.Saf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost.Bruker.Type.AKTØRID;
import static no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost.Bruker.Type.FNR;

/**
 * Dokumentasjon av SAF: https://confluence.adeo.no/display/BOA/saf
 */
@ApplicationScoped
public class SafSystemTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SafSystemTjeneste.class);
    static final String FP_DOK_TYPE = "fp_innholdtype";

    private Saf safKlient;

    SafSystemTjeneste() {
    }

    @Inject
    public SafSystemTjeneste(Saf safKlient) {
        this.safKlient = safKlient;
    }

    public Optional<EnkelJournalpost> hentJournalpostUtenDokument(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.verdi());
        var resultat = safKlient.hentJournalpostInfo(query, journalpostProjeksjon());
        return Optional.ofNullable(resultat)
            .map(SafSystemTjeneste::mapTilJournalpost);
    }

    private static JournalpostResponseProjection journalpostProjeksjon() {
        return new JournalpostResponseProjection()
            .tittel()
            .journalposttype()
            .eksternReferanseId()
            .bruker(new BrukerResponseProjection().type().id())
            .sak(new SakResponseProjection().fagsakId().fagsaksystem())
            .tilleggsopplysninger(new TilleggsopplysningResponseProjection().nokkel().verdi())
            .dokumenter(new DokumentInfoResponseProjection().tittel());
    }

    private static EnkelJournalpost mapTilJournalpost(Journalpost journalpost) {
        var innsendingstype = tilType(journalpost.getJournalposttype());
        return new EnkelJournalpost(
            journalpost.getJournalpostId(),
            journalpost.getSak().getFagsaksystem(),
            journalpost.getEksternReferanseId(),
            journalpost.getSak().getFagsakId(),
            innsendingstype,
            tilBruker(journalpost.getBruker()),
            innsendingstype.equals(JournalpostType.INNGÅENDE_DOKUMENT) ? dokumenttypeFraTilleggsopplysninger(journalpost) : null
        );
    }

    private static EnkelJournalpost.Bruker tilBruker(Bruker bruker) {
        return new EnkelJournalpost.Bruker(
            tilBrukerType(bruker.getType()),
            bruker.getId()
        );
    }

    private static EnkelJournalpost.Bruker.Type tilBrukerType(BrukerIdType type) {
        return switch (type) {
            case AKTOERID -> AKTØRID;
            case FNR -> FNR;
            case ORGNR -> throw new IllegalStateException("Innkommende dokument med brukertype " + type);
        };
    }

    private static JournalpostType tilType(Journalposttype journalposttype) {
        return switch (journalposttype) {
            case I -> JournalpostType.INNGÅENDE_DOKUMENT;
            case U -> JournalpostType.UTGÅENDE_DOKUMENT;
            case N -> throw new IllegalStateException("Utviklerfeil: Skal filterer bort notater før mapping!");
        };
    }

    private static DokumentType dokumenttypeFraTilleggsopplysninger(Journalpost journalpost) {
        return safeStream(journalpost.getTilleggsopplysninger())
            .filter(to -> FP_DOK_TYPE.equals(to.getNokkel()))
            .map(Tilleggsopplysning::getVerdi)
            .map(SafSystemTjeneste::tilDokumentTypeFraTilleggsopplysninger)
            .findFirst()
            .map(d -> utledFraTittel(journalpost.getTittel()))
            .orElse(utledFraTittel(journalpost.getDokumenter().stream().findFirst().orElseThrow().getTittel()))
            .orElse(null);
    }

    private static Optional<DokumentType> tilDokumentTypeFraTilleggsopplysninger(String dokumentType) {
        try {
            return Optional.of(DokumentType.valueOf(dokumentType));
        } catch (Exception e) {
            LOG.info("Ukjent/urelevant dokumentTypeId fra SAF tilleggsopplysninger: {}", dokumentType);
            return Optional.empty();
        }
    }

    private static Optional<DokumentType> utledFraTittel(String tittel) {
        try {
            return Optional.of(DokumentType.fraTittel(tittel));
        } catch (Exception e) {
            LOG.info("Klarte ikke utlede dokumentTypeId fra SAF tittel: {}", tittel);
            return Optional.empty();
        }
    }
}
