package no.nav.foreldrepenger.oversikt.arkiv;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.safselvbetjening.Datotype;
import no.nav.safselvbetjening.DokumentInfo;
import no.nav.safselvbetjening.DokumentInfoResponseProjection;
import no.nav.safselvbetjening.DokumentoversiktResponseProjection;
import no.nav.safselvbetjening.DokumentoversiktSelvbetjeningQueryRequest;
import no.nav.safselvbetjening.DokumentvariantResponseProjection;
import no.nav.safselvbetjening.FagsakResponseProjection;
import no.nav.safselvbetjening.Journalpost;
import no.nav.safselvbetjening.JournalpostResponseProjection;
import no.nav.safselvbetjening.Journalposttype;
import no.nav.safselvbetjening.RelevantDato;
import no.nav.safselvbetjening.RelevantDatoResponseProjection;
import no.nav.safselvbetjening.SakResponseProjection;
import no.nav.safselvbetjening.Tema;
import no.nav.vedtak.felles.integrasjon.safselvbetjening.HentDokumentQuery;
import no.nav.vedtak.felles.integrasjon.safselvbetjening.SafSelvbetjening;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class SafSelvbetjeningTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(SafSelvbetjeningTjeneste.class);
    private static final Set<Journalposttype> INKLUDER_JOURNALPOSTTYPER = Set.of(Journalposttype.I, Journalposttype.U);
    private static final List<String> GYLDIGE_FILFORMAT = List.of("PDF");

    private SafSelvbetjening safSelvbetjening;

    SafSelvbetjeningTjeneste() {
        // CDI
    }

    @Inject
    public SafSelvbetjeningTjeneste(SafSelvbetjening safSelvbetjening) {
        this.safSelvbetjening = safSelvbetjening;
    }

    public byte[] hentDokument(JournalpostId journalpostId, String dokumentId) {
        return safSelvbetjening.hentDokument(new HentDokumentQuery(journalpostId.verdi(), dokumentId));
    }

    public List<EnkelJournalpostSelvbetjening> alleJournalposter(Fødselsnummer fnr) {
        var projection = new DokumentoversiktResponseProjection()
                .journalposter(journalpostProjeksjon());

        return safSelvbetjening.dokumentoversiktSelvbetjening(query(fnr), projection).getJournalposter().stream()
                .filter(j -> INKLUDER_JOURNALPOSTTYPER.contains(j.getJournalposttype()))
                .map(SafSelvbetjeningTjeneste::tilEnkelJournalpostDto)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<EnkelJournalpostSelvbetjening> alleJournalposter(Fødselsnummer fnr, Saksnummer saksnummer) {
        var projection = new DokumentoversiktResponseProjection()
                .fagsak(fagsakProjektsjon());

        return safSelvbetjening.dokumentoversiktSelvbetjening(query(fnr), projection).getFagsak().stream()
                .filter(fagsak -> saksnummer.value().equals(fagsak.getFagsakId()))
                .flatMap(fagsak -> fagsak.getJournalposter().stream())
                .filter(j -> INKLUDER_JOURNALPOSTTYPER.contains(j.getJournalposttype()))
                .map(SafSelvbetjeningTjeneste::tilEnkelJournalpostDto)
                .flatMap(Optional::stream)
                .toList();
    }

    private static DokumentoversiktSelvbetjeningQueryRequest query(Fødselsnummer fnr) {
        var query = new DokumentoversiktSelvbetjeningQueryRequest();
        query.setIdent(fnr.value());
        query.setTema(List.of(Tema.FOR));
        return query;
    }

    private static FagsakResponseProjection fagsakProjektsjon() {
        return new FagsakResponseProjection()
                .fagsakId()
                .fagsaksystem()
                .journalposter(journalpostProjeksjon());
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

    private static Optional<EnkelJournalpostSelvbetjening> tilEnkelJournalpostDto(Journalpost journalpost) {
        var pdfDokumenter = journalpost.getDokumenter().stream()
                .filter(dokumentInfo -> dokumentInfo.getDokumentvarianter().stream()
                        .anyMatch(dokumentvariant -> dokumentvariant.getBrukerHarTilgang() && GYLDIGE_FILFORMAT.contains(dokumentvariant.getFiltype())))
                .toList();
        if (pdfDokumenter.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapTilJournalpost(journalpost, pdfDokumenter));
    }

    private static EnkelJournalpostSelvbetjening mapTilJournalpost(Journalpost journalpost, List<DokumentInfo> pdfDokument) {
        var innsendingstype = tilType(journalpost.getJournalposttype());
        return new EnkelJournalpostSelvbetjening(
                journalpost.getTittel(),
                journalpost.getJournalpostId(),
                journalpost.getSak() != null ? journalpost.getSak().getFagsakId() : null,
                innsendingstype,
                tilOpprettetDato(journalpost.getRelevanteDatoer()),
                innsendingstype.equals(JournalpostType.INNGÅENDE_DOKUMENT) ? dokumenttypeFraTittel(journalpost) : null,
                tilDokumenter(pdfDokument, journalpost.getJournalposttype())
        );
    }

    private static List<EnkelJournalpostSelvbetjening.Dokument> tilDokumenter(List<DokumentInfo> pdfDokument, Journalposttype journalposttype) {
        return pdfDokument.stream()
                .map(d -> tilDokument(d, journalposttype))
                .toList();
    }

    private static EnkelJournalpostSelvbetjening.Dokument tilDokument(DokumentInfo dokumentInfo, Journalposttype journalposttype) {
        if (journalposttype.equals(Journalposttype.U)) {
            return new EnkelJournalpostSelvbetjening.Dokument(
                    dokumentInfo.getDokumentInfoId(),
                    dokumentInfo.getTittel(),
                    tilBrevKode(dokumentInfo.getBrevkode()));
        } else {
            return new EnkelJournalpostSelvbetjening.Dokument(
                    dokumentInfo.getDokumentInfoId(),
                    dokumentInfo.getTittel(),
                    EnkelJournalpostSelvbetjening.Brevkode.URELEVANT);
        }
    }

    private static EnkelJournalpostSelvbetjening.Brevkode tilBrevKode(String brevkode) {
        try {
            return EnkelJournalpostSelvbetjening.Brevkode.fraKode(brevkode);
        } catch (Exception e) {
            LOG.info("Ukjent brevkode {}", brevkode);
            return EnkelJournalpostSelvbetjening.Brevkode.UKJENT;
        }
    }

    private static LocalDateTime tilOpprettetDato(List<RelevantDato> datoer) {
        if (datoer == null) {
            return null;
        }
        return datoer.stream()
                .filter(d -> Datotype.DATO_OPPRETTET.equals(d.getDatotype()))
                .map(RelevantDato::getDato)
                .map(date -> LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()))
                .findFirst()
                .orElseThrow();
    }

    private static JournalpostType tilType(Journalposttype journalposttype) {
        return switch (journalposttype) {
            case I -> JournalpostType.INNGÅENDE_DOKUMENT;
            case U -> JournalpostType.UTGÅENDE_DOKUMENT;
            case N -> throw new IllegalStateException("Utviklerfeil: Skal filterer bort notater før mapping!");
        };
    }

    private static DokumentType dokumenttypeFraTittel(Journalpost journalpost) {
        return utledFraTittel(journalpost.getTittel())
                .or(() -> utledFraTittel(journalpost.getDokumenter().stream().findFirst().orElseThrow().getTittel()))
                .orElse(null);
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
