package no.nav.foreldrepenger.oversikt.arkiv;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;
import no.nav.safselvbetjening.Datotype;
import no.nav.safselvbetjening.DokumentInfo;
import no.nav.safselvbetjening.DokumentInfoResponseProjection;
import no.nav.safselvbetjening.DokumentoversiktResponseProjection;
import no.nav.safselvbetjening.DokumentoversiktSelvbetjeningQueryRequest;
import no.nav.safselvbetjening.DokumentvariantResponseProjection;
import no.nav.safselvbetjening.Journalpost;
import no.nav.safselvbetjening.JournalpostResponseProjection;
import no.nav.safselvbetjening.Journalposttype;
import no.nav.safselvbetjening.RelevantDato;
import no.nav.safselvbetjening.RelevantDatoResponseProjection;
import no.nav.safselvbetjening.SakResponseProjection;
import no.nav.safselvbetjening.Tema;

/**
 * Dokumentasjon av SAF SELVBETJENING: https://confluence.adeo.no/display/BOA/safselvbetjening
 */
@ApplicationScoped
public class SafselvbetjeningTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(SafselvbetjeningTjeneste.class);
    private static final Set<Journalposttype> INKLUDER_JOURNALPOSTTYPER = Set.of(Journalposttype.I, Journalposttype.U);
    private static final List<String> GYLDIGE_FILFORMAT = List.of("PDF");
//    private static final List<String> GYLDIGE_FILFORMAT = List.of("PDF", "JPG", "PNG"); // TODO

    private SafSelvbetjening safKlient;

    SafselvbetjeningTjeneste() {
    }

    @Inject
    public SafselvbetjeningTjeneste(SafSelvbetjening safKlient) {
        this.safKlient = safKlient;
    }


    public List<EnkelJournalpost> hentAlleJournalposter(Fødselsnummer fnr, Saksnummer saksnummer) {
        var query = new DokumentoversiktSelvbetjeningQueryRequest();
        query.setIdent(fnr.value());
        query.setTema(List.of(Tema.FOR));

        var projection = new DokumentoversiktResponseProjection()
            .journalposter(journalpostProjeksjon());

        var resultat = safKlient.dokumentoversiktSelvbetjening(query, projection);

        var journalposter = resultat.getFagsak().stream()
            .filter(fagsak -> saksnummer.value().equals(fagsak.getFagsakId()))
            .flatMap(fagsak -> fagsak.getJournalposter().stream())
            .filter(j -> INKLUDER_JOURNALPOSTTYPER.contains(j.getJournalposttype()))
            .toList();

        var dokumenter = new ArrayList<EnkelJournalpost>();
        for (var journalpost : journalposter) {
            var pdfDokumenter = journalpost.getDokumenter().stream()
                .filter(dokumentInfo -> dokumentInfo.getDokumentvarianter().stream()
                    .anyMatch(dokumentvariant -> dokumentvariant.getBrukerHarTilgang() && GYLDIGE_FILFORMAT.contains(dokumentvariant.getFiltype())))
                .toList();

            if (!pdfDokumenter.isEmpty()) {
                dokumenter.add(mapTilJournalpost(journalpost, pdfDokumenter));
            }
        }
        return dokumenter;
    }

    private static JournalpostResponseProjection journalpostProjeksjon() {
        return new JournalpostResponseProjection()
            .tittel()
            .journalpostId()
            .journalposttype()
            .relevanteDatoer(new RelevantDatoResponseProjection()
                .dato()
                .datotype())
            .sak(new SakResponseProjection()
                .fagsakId()
                .fagsaksystem())
            .dokumenter(new DokumentInfoResponseProjection()
                .tittel()
                .dokumentInfoId()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection()
                    .variantformat()
                    .filtype()
                    .brukerHarTilgang()))
            .journalstatus();
    }

    private static EnkelJournalpost mapTilJournalpost(Journalpost journalpost, List<DokumentInfo> pdfDokument) {
        var innsendingstype = tilType(journalpost.getJournalposttype());
        return new EnkelJournalpost(
            journalpost.getTittel(),
            journalpost.getJournalpostId(),
            journalpost.getSak().getFagsakId(),
            null,
            innsendingstype,
            tilOpprettetDato(journalpost.getRelevanteDatoer()),
            innsendingstype.equals(EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT) ? dokumenttypeFraTittel(journalpost) : DokumentTypeId.URELEVANT,
            tilDokumenter(pdfDokument, journalpost.getJournalposttype())
        );
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

    private static LocalDateTime tilOpprettetDato(List<RelevantDato> datoer) {
        if (datoer == null) {
            return null;
        }
        return datoer.stream()
            .filter(d -> Datotype.DATO_OPPRETTET.equals(d.getDatotype()))
            .map(SafselvbetjeningTjeneste::tilLocalDateTime)
            .findFirst()
            .orElseThrow();
    }

    private static LocalDateTime tilLocalDateTime(RelevantDato relevantDato) {
        return relevantDato.getDato().toInstant()
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

    private static DokumentTypeId dokumenttypeFraTittel(Journalpost journalpost) {
        return utledFraTittel(journalpost.getTittel())
            .or(() -> utledFraTittel(journalpost.getDokumenter().stream().findFirst().orElseThrow().getTittel()))
            .orElse(DokumentTypeId.UKJENT);
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
