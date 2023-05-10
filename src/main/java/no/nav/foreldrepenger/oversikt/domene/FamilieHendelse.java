package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.common.innsyn.Familiehendelse;

public record FamilieHendelse(@JsonProperty("fødselsdato") LocalDate fødselsdato,
                              @JsonProperty("termindato") LocalDate termindato,
                              @JsonProperty("antallBarn") int antallBarn,
                              @JsonProperty("omsorgsovertakelse") LocalDate omsorgsovertakelse) {

    public Familiehendelse tilDto() {
        return new Familiehendelse(fødselsdato, termindato, antallBarn, omsorgsovertakelse);
    }
}

