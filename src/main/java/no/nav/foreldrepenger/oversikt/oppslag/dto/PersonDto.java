package no.nav.foreldrepenger.oversikt.oppslag.dto;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.common.domain.AktørId;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.domain.Navn;
import no.nav.foreldrepenger.common.domain.felles.Bankkonto;
import no.nav.foreldrepenger.common.domain.felles.Kjønn;
import no.nav.foreldrepenger.common.domain.felles.Sivilstand;
import no.nav.foreldrepenger.common.oppslag.dkif.Målform;

import java.time.LocalDate;
import java.util.List;

public record PersonDto(AktørId aktørid,
                        @NotNull Fødselsnummer fnr,
                        @NotNull LocalDate fødselsdato,
                        @NotNull Navn navn,
                        @NotNull Kjønn kjønn,
                        Målform målform,
                        Bankkonto bankkonto,
                        Sivilstand sivilstand,
                        @NotNull List<BarnDto> barn) {

    public record BarnDto(@NotNull Fødselsnummer fnr,
                          @NotNull LocalDate fødselsdato,
                          LocalDate dødsdato,
                          @NotNull Navn navn,
                          @NotNull Kjønn kjønn,
                          AnnenForelderDto annenPart) {
    }

    public record AnnenForelderDto(@NotNull Fødselsnummer fnr, @NotNull Navn navn, LocalDate fødselsdato) {
    }

}
