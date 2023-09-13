package no.nav.foreldrepenger.oversikt.arkiv;

import java.time.LocalDateTime;
import java.util.Arrays;
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
        // INNGÅENDE_BREVKODER
        SKJEMA_SVANGERSKAPSPENGER("NAV 14-04.10"),
        SKJEMA_SVANGERSKAPSPENGER_SN("NAV 14-04.10"),
        SKJEMA_FORELDREPENGER_ADOPSJON("NAV 14-05.06"),
        SKJEMA_ENGANGSSTØNAD_FØDSEL("NAV 14-05.07"),
        SKJEMA_ENGANGSSTØNAD_ADOPSJON("NAV 14-05.08"),
        SKJEMA_FORELDREPENGER_FØDSEL("NAV 14-05.09"),
        SKJEMA_FLEKSIBELT_UTTAK("NAV 14-16.05"),
        SKJEMA_INNTEKTSOPPLYSNING_SELVSTENDIG("NAV 14-35.01"),
        SKJEMA_INNTEKTSOPPLYSNINGER("NAV 08-30.01"),
        SKJEMA_KLAGE_DOKUMENT("NAV 90-00.08"),
        SKJEMA_FORELDREPENGER_ENDRING("NAV 14-05.10"),
        SKJEMAE_KLAGE("NAVe 90-00.08"),
        SKJEMA_ANNEN_POST("NAV 00-03.00"),
        SKJEMA_INNTEKTSMELDING("4936"),
        SKJEMA_TILRETTELEGGING_B("AT-474B"),
        SKJEMA_TILRETTELEGGING_N("AT-474N"),
        FORSIDE_SVP_GAMMEL("AT-474B"),

        // UTGÅENDE_BREVKODER
        FORELDREPENGER_ANNULLERT("ANUFOR"),
        FORELDREPENGER_AVSLAG("AVSFOR"),
        SVANGERSKAPSPENGER_OPPHØR("OPPSVP"),
        ENGANGSSTØNAD_INNVILGELSE("INNVES"),
        SVANGERSKAPSPENGER_AVSLAG("AVSSVP"),
        FORELDREPENGER_INNVILGELSE("INVFOR"),
        ENGANGSSTØNAD_AVSLAG("AVSLES"),
        FORELDREPENGER_OPPHØR("OPPFOR"),
        SVANGERSKAPSPENGER_INNVILGELSE("INVSVP"),

        INNHENTE_OPPLYSNINGER("INNOPP"),

        ETTERLYS_INNTEKTSMELDING("ELYSIM"),

        // Bruke?
        FORELDREPENGER_INFO_TIL_ANNEN_FORELDER("INFOAF"),
        VARSEL_OM_REVURDERING("VARREV"),
        INFO_OM_HENLEGGELSE("IOHENL"),
        INNSYN_SVAR("INNSYN"),
        IKKE_SØKT("IKKESO"),
        INGEN_ENDRING("INGEND"),
        FORLENGET_SAKSBEHANDLINGSTID("FORSAK"),
        FORLENGET_SAKSBEHANDLINGSTID_MEDL("FORMED"),
        FORLENGET_SAKSBEHANDLINGSTID_TIDLIG("FORTID"),
        KLAGE_AVVIST("KGEAVV"),
        KLAGE_OMGJORT("KGEOMG"),
        KLAGE_OVERSENDT("KGEOVE"),
        FRITEKSTBREV("FRITEK"),

        UKJENT("UKJENT");

        private String kode;

        Brevkode(String kode) {
            this.kode = kode;
        }

        public String kode() {
            return kode;
        }

        public static Brevkode fraKode(String kode) {
            return Arrays.stream(values())
                .filter(brevkode -> brevkode.kode().equals(kode))
                .findFirst()
                .orElseThrow();
        }
    }
}
