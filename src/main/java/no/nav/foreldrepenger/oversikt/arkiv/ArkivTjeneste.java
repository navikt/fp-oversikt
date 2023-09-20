package no.nav.foreldrepenger.oversikt.arkiv;


import static no.nav.foreldrepenger.oversikt.arkiv.ArkivDokumentDto.Type.INNGÅENDE_DOKUMENT;
import static no.nav.foreldrepenger.oversikt.arkiv.ArkivDokumentDto.Type.UTGÅENDE_DOKUMENT;

import java.util.Comparator;
import java.util.List;

import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public class ArkivTjeneste {

    private DokumentArkivTjeneste arkivTjeneste;

    @Inject
    public ArkivTjeneste(DokumentArkivTjeneste arkivTjeneste) {
        this.arkivTjeneste = arkivTjeneste;
    }

    public List<ArkivDokumentDto> alle(String saksnummer) {
        return arkivTjeneste.hentAlleJournalposter(new Saksnummer(saksnummer)).stream()
            .flatMap(enkelJournalpost -> enkelJournalpost.dokumenter().stream()
                .map(dokument -> tilArkivdokumenter(dokument, enkelJournalpost))
            )
            .sorted(Comparator.comparing(ArkivDokumentDto::mottatt))
            .toList();
    }

    private static ArkivDokumentDto tilArkivdokumenter(EnkelJournalpost.Dokument dokument, EnkelJournalpost enkelJournalpost) {
        return new ArkivDokumentDto(
            dokument.tittel() != null ? dokument.tittel() : enkelJournalpost.tittel(),
            tilType(enkelJournalpost.type()),
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
