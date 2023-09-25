package no.nav.foreldrepenger.oversikt.arkiv;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;

import static no.nav.foreldrepenger.oversikt.arkiv.ArkivDokumentDto.Type.INNGÅENDE_DOKUMENT;
import static no.nav.foreldrepenger.oversikt.arkiv.ArkivDokumentDto.Type.UTGÅENDE_DOKUMENT;

@ApplicationScoped
public class ArkivTjeneste {

    private SafselvbetjeningTjeneste safselvbetjeningTjeneste;

    @Inject
    public ArkivTjeneste(SafselvbetjeningTjeneste safselvbetjeningTjeneste) {
        this.safselvbetjeningTjeneste = safselvbetjeningTjeneste;
    }

    public ArkivTjeneste() {
        //CDI
    }

    public HttpResponse<byte[]> dokument(JournalpostId journalpostId, DokumentId dokumentId) {
        return safselvbetjeningTjeneste.dokument(journalpostId, dokumentId);
    }

    public List<ArkivDokumentDto> alle(Fødselsnummer fnr, Saksnummer saksnummer) {
        return tilArkivDokumenter(safselvbetjeningTjeneste.alle(fnr, saksnummer));
    }

    public List<ArkivDokumentDto> alle(Fødselsnummer fødselsnummer) {
        return tilArkivDokumenter(safselvbetjeningTjeneste.alle(fødselsnummer));

    }

    private List<ArkivDokumentDto> tilArkivDokumenter(List<EnkelJournalpost> journalposter) {
        return journalposter.stream()
            .flatMap(enkelJournalpost -> enkelJournalpost.dokumenter().stream()
                .map(dokument -> tilArkivdokument(dokument, enkelJournalpost))
            )
            .sorted(Comparator.comparing(ArkivDokumentDto::mottatt))
            .toList();
    }

    private static ArkivDokumentDto tilArkivdokument(EnkelJournalpost.Dokument dokument, EnkelJournalpost enkelJournalpost) {
        return new ArkivDokumentDto(
            dokument.tittel() != null ? dokument.tittel() : enkelJournalpost.tittel(),
            tilType(enkelJournalpost.type()),
            enkelJournalpost.saksnummer(),
            enkelJournalpost.journalpostId(),
            dokument.dokumentId(),
            enkelJournalpost.mottatt()
        );
    }

    private static ArkivDokumentDto.Type tilType(EnkelJournalpost.DokumentType type) {
        return switch (type) {
            case INNGÅENDE_DOKUMENT -> INNGÅENDE_DOKUMENT;
            case UTGÅENDE_DOKUMENT -> UTGÅENDE_DOKUMENT;
        };
    }

}
