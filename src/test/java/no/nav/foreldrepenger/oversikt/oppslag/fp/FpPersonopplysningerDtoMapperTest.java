package no.nav.foreldrepenger.oversikt.oppslag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.arbeid.Stillingsprosent;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Kjønn;
import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.Sivilstandstype;

class FpPersonopplysningerDtoMapperTest {

    private static final String SØKER_IDENT = "00000000000";
    private static final String BARN_IDENT = "11111111111";
    private static final String ANNENPART_IDENT = "22222222222";

    @Test
    void skal_mappe_person_til_dto() {
        var søker = lagPerson("Ola", null, "Nordmann", KjoennType.MANN, LocalDate.of(1990, 1, 15));
        søker.setSivilstand(List.of(new Sivilstand(Sivilstandstype.GIFT, null, null, null, null, null)));
        var søkerMedIdent = new PersonMedIdent(SØKER_IDENT, søker);

        var barn = lagPerson("Lille", null, "Nordmann", KjoennType.KVINNE, LocalDate.of(2023, 6, 1));
        var barnMedIdent = new PersonMedIdent(BARN_IDENT, barn);

        var annenpart = lagPerson("Kari", null, "Nordmann", KjoennType.KVINNE, LocalDate.of(1991, 3, 20));
        var annenpartMedIdent = new PersonMedIdent(ANNENPART_IDENT, annenpart);

        var arbeidsforhold = List.of(
            new EksternArbeidsforholdDto("123456789", "ORG", "Arbeidsgiver AS", new Stillingsprosent(BigDecimal.valueOf(100)), LocalDate.of(2020, 1, 1), null)
        );

        var dto = FpPersonopplysningerDtoMapper.tilDto(søkerMedIdent, List.of(barnMedIdent), Map.of(BARN_IDENT, annenpartMedIdent), arbeidsforhold);

        assertThat(dto.fnr().value()).isEqualTo(SØKER_IDENT);
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(dto.kjønn()).isEqualTo(Kjønn.M);
        assertThat(dto.navn().fornavn()).isEqualTo("Ola");
        assertThat(dto.navn().mellomnavn()).isNull();
        assertThat(dto.navn().etternavn()).isEqualTo("Nordmann");
        assertThat(dto.erGift()).isTrue();
        assertThat(dto.arbeidsforhold()).hasSize(1);
        assertThat(dto.arbeidsforhold().getFirst().arbeidsgiverNavn()).isEqualTo("Arbeidsgiver AS");

        assertThat(dto.barn()).hasSize(1);
        var barnDto = dto.barn().getFirst();
        assertThat(barnDto.fnr().value()).isEqualTo(BARN_IDENT);
        assertThat(barnDto.fødselsdato()).isEqualTo(LocalDate.of(2023, 6, 1));
        assertThat(barnDto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(barnDto.navn().fornavn()).isEqualTo("Lille");
        assertThat(barnDto.dødsdato()).isNull();

        assertThat(barnDto.annenPart()).isNotNull();
        assertThat(barnDto.annenPart().fnr().value()).isEqualTo(ANNENPART_IDENT);
        assertThat(barnDto.annenPart().navn().fornavn()).isEqualTo("Kari");
        assertThat(barnDto.annenPart().fødselsdato()).isEqualTo(LocalDate.of(1991, 3, 20));
    }

    private static Person lagPerson(String fornavn, String mellomnavn, String etternavn, KjoennType kjønn, LocalDate fødselsdato) {
        var person = new Person();
        person.setNavn(List.of(new Navn(fornavn, mellomnavn, etternavn, null, null, null, null, null)));
        person.setKjoenn(List.of(new Kjoenn(kjønn, null, null)));
        person.setFoedselsdato(List.of(new Foedselsdato(fødselsdato.toString(), null, null, null)));
        person.setDoedsfall(List.of());
        return person;
    }
}

