package no.nav.foreldrepenger.oversikt.oppslag.es;

import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.fødselsdatoFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.kjønnFor;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.PersonDtoMapperUtil.navnFor;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;

class EsPersonopplysningerDtoMapper {

    private EsPersonopplysningerDtoMapper() {
        // hide public constructor
    }

    static EsPersonopplysningerDto tilDto(PersonMedIdent søker) {
        return new EsPersonopplysningerDto(
                new Fødselsnummer(søker.ident()),
                fødselsdatoFor(søker),
                kjønnFor(søker),
                navnFor(søker)
        );
    }
}

