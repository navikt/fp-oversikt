package no.nav.foreldrepenger.oversikt.oppslag.felles;

import no.nav.pdl.Person;
import no.nav.vedtak.felles.integrasjon.person.FalskIdentitet;

public record PersonMedIdent(String ident, Person person, FalskIdentitet.Informasjon falskIdentitet) {
    public PersonMedIdent(String ident, Person person) {
        this(ident, person, null);
    }
}

