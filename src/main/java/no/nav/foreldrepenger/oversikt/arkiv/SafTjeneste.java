package no.nav.foreldrepenger.oversikt.arkiv;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;
import no.nav.saf.Bruker;
import no.nav.saf.BrukerIdType;
import no.nav.saf.BrukerResponseProjection;
import no.nav.saf.DokumentInfo;
import no.nav.saf.DokumentInfoResponseProjection;
import no.nav.saf.DokumentvariantResponseProjection;
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
            .map(journalpost -> mapTilJournalpost(journalpost, List.of()));
    }

    private static JournalpostResponseProjection journalpostProjeksjon() {
        return new JournalpostResponseProjection()
            .tittel()
            .journalpostId()
            .journalposttype()
            .datoOpprettet()
            .sak(new SakResponseProjection()
                .fagsakId()
                .fagsaksystem())
            .bruker(new BrukerResponseProjection()
                .id()
                .type())
            .tilleggsopplysninger(new TilleggsopplysningResponseProjection().nokkel().verdi())
            .dokumenter(new DokumentInfoResponseProjection()
                .tittel()
                .dokumentInfoId()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection().variantformat().filtype()))
            .journalstatus();
    }

    private static EnkelJournalpost mapTilJournalpost(Journalpost journalpost, List<DokumentInfo> pdfDokument) {
        var innsendingstype = tilType(journalpost.getJournalposttype());
        return new EnkelJournalpost(
            journalpost.getTittel(),
            journalpost.getJournalpostId(),
            journalpost.getSak().getFagsakId(),
            tilBruker(journalpost.getBruker()),
            innsendingstype,
            tilDato(journalpost),
            innsendingstype.equals(EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT) ? dokumenttypeFraTilleggsopplysninger(journalpost) : DokumentTypeId.URELEVANT,
            tilDokumenter(pdfDokument, journalpost.getJournalposttype())
        );
    }

    private static EnkelJournalpost.Bruker tilBruker(Bruker bruker) {
        return new EnkelJournalpost.Bruker(bruker.getId(), tilBrukerType(bruker.getType()));
    }

    private static EnkelJournalpost.Bruker.Type tilBrukerType(BrukerIdType type) {
        return switch (type) {
            case AKTOERID -> EnkelJournalpost.Bruker.Type.AKTOERID;
            case FNR -> EnkelJournalpost.Bruker.Type.FNR;
            case ORGNR -> EnkelJournalpost.Bruker.Type.ORGNR;
        };
    }

    private static List<EnkelJournalpost.Dokument> tilDokumenter(List<DokumentInfo> pdfDokument, Journalposttype journalposttype) {
        return pdfDokument.stream()
            .map(d -> tilDokument(d, journalposttype))
            .toList();
    }

    private static EnkelJournalpost.Dokument tilDokument(DokumentInfo dokumentInfo, Journalposttype journalposttype) {
        if (journalposttype.equals(Journalposttype.U)) {
            return new EnkelJournalpost.Dokument(
                dokumentInfo.getDokumentInfoId(),
                dokumentInfo.getTittel(),
                tilBrevKode(dokumentInfo.getBrevkode()));
        } else {
            return new EnkelJournalpost.Dokument(
                dokumentInfo.getDokumentInfoId(),
                dokumentInfo.getTittel(),
                EnkelJournalpost.Brevkode.URELEVANT);
        }
    }

    private static EnkelJournalpost.Brevkode tilBrevKode(String brevkode) {
        try {
            return EnkelJournalpost.Brevkode.fraKode(brevkode);
        } catch (Exception e) {
            LOG.info("Ukjent brevkode {}", brevkode);
            return EnkelJournalpost.Brevkode.UKJENT;
        }
    }

    private static LocalDateTime tilDato(Journalpost journalpost) {
        return tilLocalDateTime(journalpost.getDatoOpprettet());
    }

    private static LocalDateTime tilLocalDateTime(Date date) {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
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
