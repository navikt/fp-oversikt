package no.nav.foreldrepenger.oversikt.oppslag.oversikt;

import static no.nav.foreldrepenger.oversikt.oppslag.oversikt.OversiktPersonopplysningerDtoMapper.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;

class OversiktPersonopplysningerDtoMapperTest {

    private static final String SØKER_IDENT = "00000000000";
    private static final String BARN_IDENT = "11111111111";
    private static final String ANNENPART_IDENT = "22222222222";

    @Test
    void skal_mappe_person_til_dto() {
        var søker = lagPerson("Ola", null, "Nordmann", LocalDate.of(1990, 1, 15));
        var søkerMedIdent = new PersonMedIdent(SØKER_IDENT, søker);

        var barn = lagPerson("Lille", null, "Nordmann", LocalDate.of(2023, 6, 1));
        var barnMedIdent = new PersonMedIdent(BARN_IDENT, barn);

        var annenpart = lagPerson("Kari", null, "Nordmann", LocalDate.of(1991, 3, 20));
        var annenpartMedIdent = new PersonMedIdent(ANNENPART_IDENT, annenpart);

        var dto = tilDto(søkerMedIdent, List.of(barnMedIdent), Map.of(BARN_IDENT, annenpartMedIdent), "12345678903", true);

        assertThat(dto.fnr().value()).isEqualTo(SØKER_IDENT);
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(dto.navn().fornavn()).isEqualTo("Ola");
        assertThat(dto.navn().mellomnavn()).isNull();
        assertThat(dto.navn().etternavn()).isEqualTo("Nordmann");
        assertThat(dto.kontonummer()).isEqualTo("12345678903");
        assertThat(dto.harArbeidsforhold()).isTrue();

        assertThat(dto.barn()).hasSize(1);
        var barnDto = dto.barn().getFirst();
        assertThat(barnDto.fnr().value()).isEqualTo(BARN_IDENT);
        assertThat(barnDto.fødselsdato()).isEqualTo(LocalDate.of(2023, 6, 1));
        assertThat(barnDto.dødsdato()).isNull();
        assertThat(barnDto.navn().fornavn()).isEqualTo("Lille");
        assertThat(barnDto.navn().etternavn()).isEqualTo("Nordmann");
        assertThat(barnDto.annenPartFornavn()).isEqualTo("Kari");
    }

    @Test
    void skal_mappe_dødfødt_barn_uten_ident() {
        var søker = lagPerson("Ola", null, "Nordmann", LocalDate.of(1990, 1, 15));
        var søkerMedIdent = new PersonMedIdent(SØKER_IDENT, søker);

        var dødfødtBarn = new Person();
        dødfødtBarn.setFoedselsdato(List.of(new Foedselsdato(LocalDate.of(2024, 1, 10).toString(), null, null, null)));
        dødfødtBarn.setDoedsfall(List.of(new Doedsfall(LocalDate.of(2024, 1, 10).toString(), null, null)));
        var dødfødtMedIdent = new PersonMedIdent(null, dødfødtBarn);

        var dto = tilDto(søkerMedIdent, List.of(dødfødtMedIdent), Map.of(), null, false);

        assertThat(dto.barn()).hasSize(1);
        var barnDto = dto.barn().getFirst();
        assertThat(barnDto.fnr()).isNull();
        assertThat(barnDto.fødselsdato()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(barnDto.dødsdato()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(barnDto.navn()).isNull();
        assertThat(barnDto.annenPartFornavn()).isNull();
        assertThat(dto.kontonummer()).isNull();
    }

    @Test
    void skal_mappe_barn_uten_annenpart() {
        var søker = lagPerson("Ola", null, "Nordmann", LocalDate.of(1990, 1, 15));
        var søkerMedIdent = new PersonMedIdent(SØKER_IDENT, søker);

        var barn = lagPerson("Lille", null, "Nordmann", LocalDate.of(2023, 6, 1));
        var barnMedIdent = new PersonMedIdent(BARN_IDENT, barn);

        var dto = tilDto(søkerMedIdent, List.of(barnMedIdent), Map.of(), "12345678903", false);

        assertThat(dto.barn()).hasSize(1);
        var barnDto = dto.barn().getFirst();
        assertThat(barnDto.fnr().value()).isEqualTo(BARN_IDENT);
        assertThat(barnDto.annenPartFornavn()).isNull();
    }

    @Test
    void barn_skal_sorteres_etter_fødselsdato() {
        var søker = lagPerson("Ola", null, "Nordmann", LocalDate.of(1990, 1, 15));
        var søkerMedIdent = new PersonMedIdent(SØKER_IDENT, søker);

        var eldsteBarn = lagPerson("Eldste", null, "Nordmann", LocalDate.of(2022, 1, 1));
        var yngsteBarn = lagPerson("Yngste", null, "Nordmann", LocalDate.of(2024, 6, 1));

        var dto = tilDto(søkerMedIdent, List.of(new PersonMedIdent("33333333333", yngsteBarn), new PersonMedIdent(BARN_IDENT, eldsteBarn)), Map.of(),
            null, false);

        assertThat(dto.barn()).hasSize(2);
        assertThat(dto.barn().getFirst().fødselsdato()).isEqualTo(LocalDate.of(2022, 1, 1));
        assertThat(dto.barn().get(1).fødselsdato()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    private static Person lagPerson(String fornavn, String mellomnavn, String etternavn, LocalDate fødselsdato) {
        var person = new Person();
        person.setNavn(List.of(new Navn(fornavn, mellomnavn, etternavn, null, null, null, null, null)));
        person.setFoedselsdato(List.of(new Foedselsdato(fødselsdato.toString(), null, null, null)));
        person.setDoedsfall(List.of());
        return person;
    }
}

