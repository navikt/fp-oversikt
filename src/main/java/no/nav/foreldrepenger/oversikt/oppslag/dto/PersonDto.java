package no.nav.foreldrepenger.oversikt.oppslag.dto;

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
                        Fødselsnummer fnr,
                        LocalDate fødselsdato,
                        Navn navn,
                        Kjønn kjønn,
                        Målform målform,
                        Bankkonto bankkonto,
                        Sivilstand sivilstand,
                        List<BarnDto> barn) {

    public record BarnDto(Fødselsnummer fnr,
                          LocalDate fødselsdato,
                          LocalDate dødsdato,
                          Navn navn,
                          Kjønn kjønn,
                          AnnenForelderDto annenPart) {
    }

    public record AnnenForelderDto(Fødselsnummer fnr, Navn navn, LocalDate fødselsdato) {
    }

}
