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

    boolean avsluttet();

    Set<Aksjonspunkt> aksjonspunkt();

    FamilieHendelse familieHendelse();

    String aktørId();

    record FamilieHendelse(LocalDate fødselsdato, LocalDate termindato, int antallBarn, LocalDate omsorgsovertakelse) {

    }

    record Aksjonspunkt(Type type, Venteårsak venteårsak, LocalDateTime tidsfrist) {

        public enum Venteårsak {
            ANKE_VENTER_PÅ_MERKNADER_FRA_BRUKER,
            AVVENT_DOKUMTANSJON,
            AVVENT_FØDSEL,
            AVVENT_RESPONS_REVURDERING,
            BRUKERTILBAKEMELDING,
            UTLAND_TRYGD,
            FOR_TIDLIG_SOKNAD,
            UTVIDET_FRIST,
            INNTEKT_RAPPORTERINGSFRIST,
            MANGLENDE_SYKEMELDING,
            MANGLENDE_INNTEKTSMELDING,
            OPPTJENING_OPPLYSNINGER,
            SISTE_AAP_ELLER_DP_MELDEKORT,
            SENDT_INFORMASJONSBREV,
            ÅPEN_BEHANDLING,
        }

        public enum Type {
            VENT_MANUELT_SATT,
            VENT_FØDSEL,
            VENT_KOMPLETT_SØKNAD,
            VENT_REVURDERING,
            VENT_TIDLIG_SØKNAD,
            VENT_KØET_BEHANDLING,
            VENT_SØKNAD,
            VENT_INNTEKT_RAPPORTERINGSFRIST,
            VENT_SISTE_AAP_ELLER_DP_MELDEKORT,
            VENT_ETTERLYST_INNTEKTSMELDING,
            VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN,
            VENT_SYKEMELDING,
            VENT_KABAL_KLAGE,
            VENT_PÅ_KABAL_ANKE,
        }
    }
}
