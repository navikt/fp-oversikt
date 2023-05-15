package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FpSak.class, name = "foreldrepenger"),
    @JsonSubTypes.Type(value = SvpSak.class, name = "svangerskapspenger"),
    @JsonSubTypes.Type(value = EsSak.class, name = "engangsstønad"),
})
public interface Sak {

    String saksnummer();

    Status status();

    Set<Aksjonspunkt> aksjonspunkt();

    FamilieHendelse familieHendelse();

    String aktørId();

    Set<Egenskap> egenskaper();

    record FamilieHendelse(LocalDate fødselsdato, LocalDate termindato, int antallBarn, LocalDate omsorgsovertakelse) {

    }

    enum Status {
        OPPRETTET,
        UNDER_BEHANDLING,
        LØPENDE,
        AVSLUTTET,
    }

    record Aksjonspunkt(String kode, Aksjonspunkt.Status status, String venteÅrsak, LocalDateTime opprettetTidspunkt) {
        public enum Status {
            UTFØRT,
            OPPRETTET,
        }
    }

    enum Egenskap {
        SØKNAD_UNDER_BEHANDLING,
    }
}
