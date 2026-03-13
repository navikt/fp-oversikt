package no.nav.foreldrepenger.oversikt.oppslag.oversikt;

import static java.util.Comparator.comparing;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.dødsdatoFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.fødselsdatoFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.navnFor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;

class OversiktPersonopplysningerDtoMapper {

    private OversiktPersonopplysningerDtoMapper() {
        // hide public constructor
    }

    static OversiktPersonopplysningerDto tilDto(PersonMedIdent søker,
                                                List<PersonMedIdent> barn,
                                                Map<String, PersonMedIdent> annenpart,
                                                String kontonummer,
                                                boolean harArbeidsforhold) {
        return new OversiktPersonopplysningerDto(new Fødselsnummer(søker.ident()), fødselsdatoFor(søker), navnFor(søker), kontonummer,
            harArbeidsforhold, tilBarn(barn, annenpart));
    }

    private static List<OversiktPersonopplysningerDto.OversiktBarnDto> tilBarn(List<PersonMedIdent> barn, Map<String, PersonMedIdent> annenpart) {
        return Stream.ofNullable(barn)
            .flatMap(Collection::stream)
            .map(barnet -> tilBarn(barnet, annenpart))
            .sorted(comparing(OversiktPersonopplysningerDto.OversiktBarnDto::fødselsdato))
            .toList();
    }

    private static OversiktPersonopplysningerDto.OversiktBarnDto tilBarn(PersonMedIdent barnet, Map<String, PersonMedIdent> annenpart) {
        if (barnet.ident() == null) { // Dødfødt barn
            return new OversiktPersonopplysningerDto.OversiktBarnDto(null, fødselsdatoFor(barnet), dødsdatoFor(barnet), null, null);
        }

        return new OversiktPersonopplysningerDto.OversiktBarnDto(new Fødselsnummer(barnet.ident()), fødselsdatoFor(barnet), dødsdatoFor(barnet),
            navnFor(barnet), annenpart.containsKey(barnet.ident()) ? navnFor(annenpart.get(barnet.ident())).fornavn() : null);
    }
}
