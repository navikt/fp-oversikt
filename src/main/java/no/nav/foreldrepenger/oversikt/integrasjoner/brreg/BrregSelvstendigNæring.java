package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

import java.time.LocalDate;
import java.util.List;

public record BrregSelvstendigNæring(String organisasjonsnummer,
                                     String navn,
                                     String organisasjonsformKode,
                                     String organisasjonsformBeskrivelse,
                                     VirksomhetType virksomhetType,
                                     Boolean underAvvikling,
                                     LocalDate stiftelsesdato,
                                     LocalDate registreringsdatoEnhetsregisteret,
                                     List<SNRolleType> roller) {


    @Override
    public String toString() {
        return "BrregEnhetDto{" + "organisasjonsnummer='" + organisasjonsnummer + '\'' + ", organisasjonsform=" + organisasjonsformKode + '}';
    }
}
