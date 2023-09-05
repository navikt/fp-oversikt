package no.nav.foreldrepenger.oversikt.arkiv;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;
import no.nav.saf.DokumentInfoResponseProjection;
import no.nav.saf.DokumentvariantResponseProjection;
import no.nav.saf.Journalpost;
import no.nav.saf.JournalpostQueryRequest;
import no.nav.saf.JournalpostResponseProjection;
import no.nav.saf.LogiskVedleggResponseProjection;
import no.nav.saf.SakResponseProjection;
import no.nav.saf.Tilleggsopplysning;
import no.nav.saf.TilleggsopplysningResponseProjection;
import no.nav.vedtak.felles.integrasjon.saf.Saf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

@ApplicationScoped
public class DokumentArkivTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentArkivTjeneste.class);

    private static final String FP_DOK_TYPE = "fp_innholdtype";
    private Saf safKlient;

    DokumentArkivTjeneste() {
    }

    @Inject
    public DokumentArkivTjeneste(Saf safKlient) {
        this.safKlient = safKlient;
    }


    public Optional<EnkelJournalpost> hentJournalpost(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.verdi());
        var projection = new JournalpostResponseProjection()
            .sak(new SakResponseProjection()
                    .fagsakId()
                    .fagsaksystem())
            .journalpostId()
            .journalposttype()
            .tilleggsopplysninger(new TilleggsopplysningResponseProjection().nokkel().verdi())
            .dokumenter(new DokumentInfoResponseProjection()
                    .tittel()
                    .dokumentInfoId()
                    .brevkode()
                    .logiskeVedlegg(new LogiskVedleggResponseProjection().tittel())
                    .dokumentvarianter(new DokumentvariantResponseProjection().variantformat()))
            .journalstatus();
        var resultat = safKlient.hentJournalpostInfo(query, projection);
        return Optional.ofNullable(resultat).map(DokumentArkivTjeneste::mapTilJournalpost);
    }

    private static EnkelJournalpost mapTilJournalpost(Journalpost journalpost) {
        var doktypeFraTilleggsopplysning = safeStream(journalpost.getTilleggsopplysninger())
            .filter(to -> FP_DOK_TYPE.equals(to.getNokkel()))
            .map(Tilleggsopplysning::getVerdi)
            .collect(Collectors.toSet());
        var saksnummer = journalpost.getSak().getFagsakId();
        // dokumenttypeId for vedlegg ligger nok ikke på jp, men må ev utledes basert på tittel
        // se tilsvarende i historikk/fpsak/fpfordel
        var antallDokumenter = journalpost.getDokumenter() == null ? 0 : journalpost.getDokumenter().size();
        var msg = String.format("Mappet journalpost %s, antall dokumenter %s, system %s, med dokumentTyper %s",
            journalpost.getJournalpostId(), antallDokumenter, journalpost.getSak().getFagsaksystem(),
            doktypeFraTilleggsopplysning);
        LOG.info(msg);
        return new EnkelJournalpost(saksnummer, tilDokumentType(doktypeFraTilleggsopplysning));
    }

    private static Set<DokumentType> tilDokumentType(Set<String> doktypeFraTilleggsopplysning) {
        return doktypeFraTilleggsopplysning.stream()
                .map(DokumentType::valueOf)
                .collect(Collectors.toSet());
    }
}
