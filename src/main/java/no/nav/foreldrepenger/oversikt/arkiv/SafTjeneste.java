package no.nav.foreldrepenger.oversikt.arkiv;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.util.Optional;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.saf.AvsenderMottakerIdType;
import no.nav.saf.AvsenderMottakerResponseProjection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
            .sak(new SakResponseProjection().fagsakId())
            .avsenderMottaker(new AvsenderMottakerResponseProjection().id().erLikBruker().type())
            .tilleggsopplysninger(new TilleggsopplysningResponseProjection().nokkel().verdi())
            .dokumenter(new DokumentInfoResponseProjection().tittel());
    }

    private static EnkelJournalpost mapTilJournalpost(Journalpost journalpost) {
        var innsendingstype = tilType(journalpost.getJournalposttype());
        var avsenderMottaker = journalpost.getAvsenderMottaker();
        var fnr = avsenderMottaker.getErLikBruker() && avsenderMottaker.getType() == AvsenderMottakerIdType.FNR ? new Fødselsnummer(avsenderMottaker.getId()) : null;
        return new EnkelJournalpost(
            journalpost.getSak().getFagsakId(),
            innsendingstype,
            fnr,
            innsendingstype.equals(EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT) ? dokumenttypeFraTilleggsopplysninger(journalpost) : DokumentTypeId.URELEVANT
        );
    }

    private static EnkelJournalpost.DokumentType tilType(Journalposttype journalposttype) {
        return switch (journalposttype) {
            case I -> EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT;
            case U -> EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT;
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
