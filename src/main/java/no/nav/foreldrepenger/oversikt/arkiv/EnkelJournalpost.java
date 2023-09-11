package no.nav.foreldrepenger.oversikt.arkiv;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;

public record EnkelJournalpost(String tittel,
                               String journalpostId,
                               String saksnummer,
                               Bruker bruker,
                               DokumentType type,
                               LocalDateTime mottatt,
                               DokumentTypeId hovedtype,
                               KildeSystem kildeSystem,
                               List<Dokument> dokumenter) {
    public record Bruker(String id, Type type) {
        public enum Type {
            AKTOERID,
            FNR,
            ORGNR
        }
    }

    public enum DokumentType {
        INNGÅENDE_DOKUMENT,
        UTGÅENDE_DOKUMENT
    }

    public enum KildeSystem {
        FPTILBAKE, ANNET
    }

    public record Dokument(String dokumentId, DokumentTypeId dokumentTypeId, Brevkode brevkode) {
    }

    public enum Brevkode {
        INNVES, // Vedtak om innvilgelse av engangsstønad
        AVSSVP, // Avslagsbrev svangerskapspenger
        INVFOR, // Innvilgelsesbrev Foreldrepenger
        AVSLES, // Avslag engangsstønad
        OPPFOR, // Opphør Foreldrepenger
        INVSVP, // Innvilgelsesbrev svangerskapspenger
        ANUFOR, // Annullering av Foreldrepenger
        AVSFOR, // Avslagsbrev Foreldrepenger
        OPPSVP, // Opphørsbrev svangerskapspenger
        INNOPP, // Innhent opplysninger
        ELYSIM, // Etterlys inntektsmelding
        UKJENT;
    }
}
