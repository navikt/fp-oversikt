package no.nav.foreldrepenger.oversikt.oppslag.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.oppslag.felles.Kjønn;
import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;

class EsPersonopplysningerDtoMapperTest {

    private static final String IDENT = "00000000000";

    @Test
    void skal_mappe_person_til_dto() {
        var person = new Person();
        person.setNavn(List.of(new Navn("Ola", "Mellomnavn", "Nordmann", null, null, null, null, null)));
        person.setKjoenn(List.of(new Kjoenn(KjoennType.MANN, null, null)));
        person.setFoedselsdato(List.of(new Foedselsdato(LocalDate.of(1990, 1, 15).toString(), null, null, null)));
        var personMedIdent = new PersonMedIdent(IDENT, person);

        var dto = EsPersonopplysningerDtoMapper.tilDto(personMedIdent);

        assertThat(dto.fnr().value()).isEqualTo(IDENT);
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(dto.kjønn()).isEqualTo(Kjønn.M);
        assertThat(dto.navn().fornavn()).isEqualTo("Ola");
        assertThat(dto.navn().mellomnavn()).isEqualTo("Mellomnavn");
        assertThat(dto.navn().etternavn()).isEqualTo("Nordmann");
    }

}

