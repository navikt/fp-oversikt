package no.nav.foreldrepenger.oversikt.oppslag.felles;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.pdl.KjoennType;
import no.nav.vedtak.felles.integrasjon.person.FalskIdentitet;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;

public class PersonDtoMapperUtil {

    private PersonDtoMapperUtil() {
        // hide public constructor
    }

    public static Kjønn kjønnFor(PersonMedIdent person) {
        var kjønn = Optional.ofNullable(person.falskIdentitet()).map(FalskIdentitet.Informasjon::kjønn)
                .or(() -> Optional.of(PersonMappers.mapKjønn(person.person())))
                .orElse(KjoennType.UKJENT);
        return switch (kjønn) {
            case MANN -> Kjønn.M;
            case KVINNE -> Kjønn.K;
            case UKJENT -> Kjønn.U;
        };
    }

    public static Navn navnFor(PersonMedIdent person) {
        return Optional.ofNullable(person.falskIdentitet()).map(PersonDtoMapperUtil::navn)
                .or(() -> Stream.ofNullable(person.person().getNavn())
                        .flatMap(Collection::stream)
                        .filter(Objects::nonNull)
                        .map(PersonDtoMapperUtil::navn)
                        .findFirst())
                .orElse(null);
    }

    public static LocalDate fødselsdatoFor(PersonMedIdent personMedIdent) {
        return Optional.ofNullable(personMedIdent.falskIdentitet()).map(FalskIdentitet.Informasjon::fødselsdato)
                .or(() -> PersonMappers.mapFødselsdato(personMedIdent.person()))
                .orElse(null);
    }

    public static LocalDate dødsdatoFor(PersonMedIdent person) {
        return PersonMappers.mapDødsdato(person.person()).orElse(null);
    }

    private static Navn navn(no.nav.pdl.Navn navn) {
        return new Navn(
                PersonMappers.titlecaseNavn(navn.getFornavn()),
                PersonMappers.titlecaseNavn(navn.getMellomnavn()),
                PersonMappers.titlecaseNavn(navn.getEtternavn())
        );
    }

    private static Navn navn(FalskIdentitet.Informasjon falskId) {
        return new Navn(
                falskId.fornavn(),
                falskId.mellomnavn(),
                falskId.etternavn()
        );
    }
}

