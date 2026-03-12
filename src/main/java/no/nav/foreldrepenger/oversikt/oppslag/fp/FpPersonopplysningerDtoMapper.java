package no.nav.foreldrepenger.oversikt.oppslag.fp;

import static java.util.Comparator.comparing;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.dødsdatoFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.fødselsdatoFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.kjønnFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.navnFor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;
import no.nav.pdl.Person;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.Sivilstandstype;

class FpPersonopplysningerDtoMapper {

    private FpPersonopplysningerDtoMapper() {
        // hide public constructor
    }

    static FpPersonopplysningerDto tilDto(PersonMedIdent søker,
                                          List<PersonMedIdent> barn,
                                          Map<String, PersonMedIdent> annenpart,
                                          List<EksternArbeidsforholdDto> arbeidsforhold) {
        var søkerPerson = søker.person();
        return new FpPersonopplysningerDto(new Fødselsnummer(søker.ident()), fødselsdatoFor(søker), kjønnFor(søker), navnFor(søker), erGift(søkerPerson),
            tilBarn(barn, annenpart), arbeidsforhold);
    }

    private static boolean erGift(Person søkerPerson) {
        return Stream.ofNullable(søkerPerson.getSivilstand())
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .map(Sivilstand::getType)
            .findFirst()
            .map(type -> type == Sivilstandstype.GIFT)
            .orElse(false);
    }

    private static List<FpPersonopplysningerDto.BarnDto> tilBarn(List<PersonMedIdent> barn,
                                                                 Map<String, PersonMedIdent> annenpart) {
        return Stream.ofNullable(barn)
            .flatMap(Collection::stream)
            .map(barnet -> tilBarn(barnet, annenpart))
            .sorted(comparing(FpPersonopplysningerDto.BarnDto::fødselsdato))
            .toList();
    }

    private static FpPersonopplysningerDto.BarnDto tilBarn(PersonMedIdent barnet, Map<String, PersonMedIdent> annenpart) {
        if (barnet.ident() == null) { // Dødfødt barn
            return new FpPersonopplysningerDto.BarnDto(null, fødselsdatoFor(barnet), dødsdatoFor(barnet), null, null, null);
        }

        return new FpPersonopplysningerDto.BarnDto(new Fødselsnummer(barnet.ident()), fødselsdatoFor(barnet), dødsdatoFor(barnet), navnFor(barnet),
            kjønnFor(barnet), annenpart.containsKey(barnet.ident()) ? tilAnnenpart(annenpart.get(barnet.ident())) : null);
    }

    private static FpPersonopplysningerDto.AnnenForelderDto tilAnnenpart(PersonMedIdent personMedIdent) {
        return new FpPersonopplysningerDto.AnnenForelderDto(new Fødselsnummer(personMedIdent.ident()), navnFor(personMedIdent), fødselsdatoFor(personMedIdent));
    }
}

