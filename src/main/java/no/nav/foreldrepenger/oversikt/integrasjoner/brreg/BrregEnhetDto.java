package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

import java.time.LocalDate;

public record BrregEnhetDto(String organisasjonsnummer,
                            String navn,
                            EnhetKodeDto organisasjonsform,
                            EnhetKodeDto naeringskode1,
                            EnhetKodeDto naeringskode2,
                            EnhetKodeDto naeringskode3,
                            Boolean underAvvikling,
                            String overordnetEnhet,
                            LocalDate stiftelsesdato,
                            LocalDate registreringsdatoEnhetsregisteret,
                            EnhetLinksDto _links) {

    public record EnhetLinksDto(EnhetLinkDto self, EnhetLinkDto overordnetEnhet) {}

    public record EnhetLinkDto(String href) {}

    public record EnhetKodeDto(String kode, String beskrivelse) {}

    @Override
    public String toString() {
        return "BrregEnhetDto{" + "organisasjonsnummer='" + organisasjonsnummer + '\'' + ", organisasjonsform=" + organisasjonsform
            + ", overordnetEnhet='" + overordnetEnhet + '\'' + ", _links=" + _links + '}';
    }
}
