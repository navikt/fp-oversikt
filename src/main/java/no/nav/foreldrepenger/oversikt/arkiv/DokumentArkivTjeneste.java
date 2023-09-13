package no.nav.foreldrepenger.oversikt.arkiv;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;
import no.nav.saf.Bruker;
import no.nav.saf.BrukerIdType;
import no.nav.saf.BrukerResponseProjection;
import no.nav.saf.DokumentInfo;
import no.nav.saf.DokumentInfoResponseProjection;
import no.nav.saf.DokumentoversiktFagsakQueryRequest;
import no.nav.saf.DokumentoversiktResponseProjection;
import no.nav.saf.DokumentvariantResponseProjection;
import no.nav.saf.FagsakInput;
import no.nav.saf.Journalpost;
import no.nav.saf.JournalpostQueryRequest;
import no.nav.saf.JournalpostResponseProjection;
import no.nav.saf.Journalposttype;
import no.nav.saf.SakResponseProjection;
import no.nav.saf.Tilleggsopplysning;
import no.nav.saf.TilleggsopplysningResponseProjection;
import no.nav.vedtak.felles.integrasjon.saf.Saf;

/**
 * Dokumentasjon av SAF: https://confluence.adeo.no/display/BOA/saf+-+GraphQL+API+v1
 */
@ApplicationScoped
public class DokumentArkivTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentArkivTjeneste.class);
    private static final Set<Journalposttype> INKLUDER_JOURNALPOSTTYPER = Set.of(Journalposttype.I, Journalposttype.U);
//    private static final Set<Journalstatus> INKLUDER_STATUS = Set.of(Journalstatus.MOTTATT); TODO
//    private static final String EKSKLUDER_DOKUMENT_MERKERT_FOR_SLETTING = "FEIL"; TODO
    private static final String FAGSAKSYSTEM_FPSAK_KODE = "FS36";
    private static final List<String> GYLDIGE_FILFORMAT = List.of("PDF");
//    private static final List<String> GYLDIGE_FILFORMAT = List.of("PDF", "JPG", "PNG"); // TODO
    static final String FP_DOK_TYPE = "fp_innholdtype";
    private static final String BEHANDLINGTEMA_TILBAKEBETALING = "ab0007";

    private Saf safKlient;

    DokumentArkivTjeneste() {
    }

    @Inject
    public DokumentArkivTjeneste(Saf safKlient) {
        this.safKlient = safKlient;
    }


    public List<EnkelJournalpost> hentAlleJournalposter(Saksnummer saksnummer) {
        // TODO: Implementer cache?

        var query = new DokumentoversiktFagsakQueryRequest();
        query.setFagsak(new FagsakInput(saksnummer.value(), FAGSAKSYSTEM_FPSAK_KODE));
        query.setFoerste(1000);

        var projection = new DokumentoversiktResponseProjection()
            .journalposter(journalpostProjeksjon());

        var resultat = safKlient.dokumentoversiktFagsak(query, projection);

        var journalposter = resultat.getJournalposter().stream()
            .filter(j -> INKLUDER_JOURNALPOSTTYPER.contains(j.getJournalposttype()))
            // .filter(j -> INKLUDER_JOURNALSTATUS.contains(j.getJournalstatus())) TODO
            // .filter(j -> !EKSKLUDER_DOKUMENT_MERKERT_FOR_SLETTING.equals(j.getSkjerming())) TODO
            .toList();

        var dokumenter = new ArrayList<EnkelJournalpost>();
        for (var journalpost : journalposter) {
            var pdfDokumenter = journalpost.getDokumenter().stream()
                .filter(dokumentInfo -> dokumentInfo.getDokumentvarianter().stream()
                    .anyMatch(dokumentvariant -> GYLDIGE_FILFORMAT.contains(dokumentvariant.getFiltype())))
                .toList();

            if (!pdfDokumenter.isEmpty()) {
                dokumenter.add(mapTilJournalpost(journalpost, pdfDokumenter));
            }
        }
        return dokumenter;
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
            .behandlingstema()
            .sak(new SakResponseProjection()
                .fagsakId()
                .fagsaksystem())
            .bruker(new BrukerResponseProjection()
                .id()
                .type())
            .skjerming()
            .tilleggsopplysninger(new TilleggsopplysningResponseProjection().nokkel().verdi())
            .dokumenter(new DokumentInfoResponseProjection()
                .tittel()
                .dokumentInfoId()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection().variantformat().filtype()))
            .journalstatus();
    }

    private static EnkelJournalpost mapTilJournalpost(Journalpost journalpost, List<DokumentInfo> pdfDokument) {
        return new EnkelJournalpost(
            journalpost.getTittel(),
            journalpost.getJournalpostId(),
            journalpost.getSak().getFagsakId(),
            tilBruker(journalpost.getBruker()),
            tilType(journalpost.getJournalposttype()),
            tilDato(journalpost),
            dokumenttypeFraTilleggsopplysninger(journalpost),
            tilKildeSystem(journalpost),
            tilDokumenter(pdfDokument)
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

    private static List<EnkelJournalpost.Dokument> tilDokumenter(List<DokumentInfo> pdfDokument) {
        return pdfDokument.stream()
            .map(DokumentArkivTjeneste::tilDokument)
            .toList();
    }

    private static EnkelJournalpost.Dokument tilDokument(DokumentInfo dokumentInfo) {
        return new EnkelJournalpost.Dokument(
            dokumentInfo.getDokumentInfoId(),
            tilDokumentType(dokumentInfo.getTittel()),
            tilBrevKode(dokumentInfo.getBrevkode()));
    }

    private static EnkelJournalpost.Brevkode tilBrevKode(String brevkode) {
        if (brevkode == null) { // Gjelder samtlige inngående dokumenter
            return EnkelJournalpost.Brevkode.UKJENT;
        }

        try {
            return EnkelJournalpost.Brevkode.valueOf(brevkode);
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

    private static EnkelJournalpost.KildeSystem tilKildeSystem(Journalpost journalpost) {
        return Objects.equals(journalpost.getBehandlingstema(), BEHANDLINGTEMA_TILBAKEBETALING) ? EnkelJournalpost.KildeSystem.FPTILBAKE :
            EnkelJournalpost.KildeSystem.ANNET;
    }

    private static DokumentTypeId dokumenttypeFraTilleggsopplysninger(Journalpost journalpost) {
        return safeStream(journalpost.getTilleggsopplysninger())
            .filter(to -> FP_DOK_TYPE.equals(to.getNokkel()))
            .map(Tilleggsopplysning::getVerdi)
            .map(DokumentArkivTjeneste::tilDokumentType)
            .findFirst()
            .orElse(DokumentTypeId.URELEVANT);
    }

    private static DokumentTypeId tilDokumentType(String dokumentType) {
        try {
            return DokumentTypeId.valueOf(dokumentType);
        } catch (Exception e) {
            LOG.info("Ukjent/urelevant dokumentTypeId fra SAF: {}", dokumentType);
            return DokumentTypeId.URELEVANT;
        }
    }
}
