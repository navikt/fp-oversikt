package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArbeidsforholdRS(String arbeidsforholdId,
                               Long navArbeidsforholdId,
                               OpplysningspliktigArbeidsgiverRS arbeidsgiver,
                               AnsettelsesperiodeRS ansettelsesperiode,
                               List<ArbeidsavtaleRS> arbeidsavtaler,
                               List<PermisjonPermitteringRS> permisjonPermitteringer,
                               ArbeidType type) {


    public record ArbeidsavtaleRS(BigDecimal stillingsprosent, PeriodeRS gyldighetsperiode) { }

    public record OpplysningspliktigArbeidsgiverRS(OpplysningspliktigType type,
                                                   String organisasjonsnummer,
                                                   String aktoerId,
                                                   String offentligIdent) {
    }

    public record PermisjonPermitteringRS(PeriodeRS periode, BigDecimal prosent, PermType type) { }

    public enum OpplysningspliktigType {
        @JsonProperty("Organisasjon") ORGANISASJON,
        @JsonProperty("Person") PERSON
    }

    public record AnsettelsesperiodeRS(PeriodeRS periode) { }

    public record PeriodeRS(LocalDate fom, LocalDate tom) { }

    public List<PermisjonPermitteringRS> getPermisjonPermitteringer() {
        return permisjonPermitteringer != null ? permisjonPermitteringer : List.of();
    }
}
