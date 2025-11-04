package no.nav.foreldrepenger.oversikt.oppslag.dto;

import static no.nav.vedtak.util.InputValideringRegex.FRITEKST;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.validation.constraints.Pattern;

@JsonPropertyOrder({"fornavn", "mellomnavn", "etternavn"})
public record Navn(@Pattern(regexp = FRITEKST) String fornavn,
                   @Pattern(regexp = FRITEKST) String mellomnavn,
                   @Pattern(regexp = FRITEKST) String etternavn) {

    @JsonIgnore
    public String navn() {
        return Stream.of(fornavn, mellomnavn, etternavn)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    @Override
    public String toString() {
        return "Navn maskert";
    }
}
