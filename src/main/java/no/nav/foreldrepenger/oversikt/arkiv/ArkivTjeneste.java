package no.nav.foreldrepenger.oversikt.arkiv;


import static no.nav.foreldrepenger.oversikt.arkiv.ArkivDokumentDto.Type.INNGÅENDE_DOKUMENT;
import static no.nav.foreldrepenger.oversikt.arkiv.ArkivDokumentDto.Type.UTGÅENDE_DOKUMENT;

import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

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

    public List<ArkivDokumentDto> alle(Fødselsnummer fnr, Saksnummer saksnummer) {
        return safselvbetjeningTjeneste.hentAlleJournalposter(fnr, saksnummer).stream()
            .filter(j -> j.saksnummer().equals(saksnummer.value()))
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
