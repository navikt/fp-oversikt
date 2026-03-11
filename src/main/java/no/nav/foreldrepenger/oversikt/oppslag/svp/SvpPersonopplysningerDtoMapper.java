package no.nav.foreldrepenger.oversikt.oppslag.svp;

import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.fødselsdatoFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.kjønnFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.navnFor;

import java.util.List;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;

class SvpPersonopplysningerDtoMapper {

    private SvpPersonopplysningerDtoMapper() {
        // hide public constructor
    }

    static SvpPersonopplysningerDto tilDto(PersonMedIdent søker, List<EksternArbeidsforholdDto> arbeidsforhold) {
        return new SvpPersonopplysningerDto(
                new Fødselsnummer(søker.ident()),
                fødselsdatoFor(søker),
                kjønnFor(søker),
                navnFor(søker),
                arbeidsforhold
        );
    }
}

