package no.nav.foreldrepenger.oversikt.arkiv;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost.Bruker.Type.AKTØRID;
import static no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost.Bruker.Type.FNR;
import static no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT;
import static no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

/**
 * Dokumentasjon av SAF: https://confluence.adeo.no/display/BOA/saf
 */
@ApplicationScoped
public class SafTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SafTjeneste.class);
    static final String FP_DOK_TYPE = "fp_innholdtype";

    private Saf safKlient;

    SafTjeneste() {
    }

    @Inject
    public SafTjeneste(Saf safKlient) {
        this.safKlient = safKlient;
    }

    public Optional<EnkelJournalpost> hentJournalpostUtenDokument(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.verdi());
        var resultat = safKlient.hentJournalpostInfo(query, journalpostProjeksjon());
        return Optional.ofNullable(resultat)
            .map(SafTjeneste::mapTilJournalpost);
    }

    private static JournalpostResponseProjection journalpostProjeksjon() {
        return new JournalpostResponseProjection()
            .tittel()
            .journalposttype()
            .eksternReferanseId()
            .bruker(new BrukerResponseProjection().type().id())
            .sak(new SakResponseProjection().fagsakId())
            .tilleggsopplysninger(new TilleggsopplysningResponseProjection().nokkel().verdi())
            .dokumenter(new DokumentInfoResponseProjection().tittel());
    }

    private static EnkelJournalpost mapTilJournalpost(Journalpost journalpost) {
        var innsendingstype = tilType(journalpost.getJournalposttype());
        return new EnkelJournalpost(
            journalpost.getJournalpostId(),
            journalpost.getEksternReferanseId(),
            journalpost.getSak().getFagsakId(),
            innsendingstype,
            tilBruker(journalpost.getBruker()),
            innsendingstype.equals(INNGÅENDE_DOKUMENT) ? dokumenttypeFraTilleggsopplysninger(journalpost) : DokumentTypeId.URELEVANT
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

    private static EnkelJournalpost.DokumentType tilType(Journalposttype journalposttype) {
        return switch (journalposttype) {
            case I -> INNGÅENDE_DOKUMENT;
            case U -> UTGÅENDE_DOKUMENT;
            case N -> throw new IllegalStateException("Utviklerfeil: Skal filterer bort notater før mapping!");
        };
    }

    private static DokumentTypeId dokumenttypeFraTilleggsopplysninger(Journalpost journalpost) {
        return safeStream(journalpost.getTilleggsopplysninger())
            .filter(to -> FP_DOK_TYPE.equals(to.getNokkel()))
            .map(Tilleggsopplysning::getVerdi)
            .map(SafTjeneste::tilDokumentTypeFraTilleggsopplysninger)
            .findFirst()
            .map(d -> utledFraTittel(journalpost.getTittel()))
            .orElse(utledFraTittel(journalpost.getDokumenter().stream().findFirst().orElseThrow().getTittel()))
            .orElse(DokumentTypeId.UKJENT);
    }

    private static Optional<DokumentTypeId> tilDokumentTypeFraTilleggsopplysninger(String dokumentType) {
        try {
            return Optional.of(DokumentTypeId.valueOf(dokumentType));
        } catch (Exception e) {
            LOG.info("Ukjent/urelevant dokumentTypeId fra SAF tilleggsopplysninger: {}", dokumentType);
            return Optional.empty();
        }
    }

    private static Optional<DokumentTypeId> utledFraTittel(String tittel) {
        try {
            return Optional.of(DokumentTypeId.fraTittel(tittel));
        } catch (Exception e) {
            LOG.info("Klarte ikke utlede dokumentTypeId fra SAF tittel: {}", tittel);
            return Optional.empty();
        }
    }
}
