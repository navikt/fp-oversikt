package no.nav.foreldrepenger.oversikt.oppslag.mapper;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.domain.felles.Bankkonto;
import no.nav.foreldrepenger.common.domain.felles.Kjønn;
import no.nav.foreldrepenger.common.domain.felles.Sivilstand;
import no.nav.foreldrepenger.common.oppslag.dkif.Målform;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer.KontonummerDto;
import no.nav.foreldrepenger.oversikt.oppslag.PdlOppslagTjeneste;
import no.nav.foreldrepenger.oversikt.oppslag.dto.PersonDto;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Comparator.comparing;
import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

public class PersonDtoMapper {

    private PersonDtoMapper() {
        // hide public constructor
    }

    public static PersonDto map() {
        return null;
    }

    public static PersonDto tilPersonDto(AktørId aktøridSøker, PdlOppslagTjeneste.PersonMedIdent søker,
                                         List<PdlOppslagTjeneste.PersonMedIdent> barn,
                                         Map<String, PdlOppslagTjeneste.PersonMedIdent> annenpart,
                                         Målform målform,
                                         KontonummerDto kontonummer) {
        var søkerPerson = søker.person();
        var fødselsdato = fødselsdatoFor(søkerPerson);
        return new PersonDto(
                new no.nav.foreldrepenger.common.domain.AktørId(aktøridSøker.value()),
                new Fødselsnummer(søker.ident()),
                fødselsdato,
                navnFor(søkerPerson),
                kjønnFor(søkerPerson),
                målform,
                tilBankkonto(kontonummer),
                tilSivilstand(søkerPerson),
                tilBarn(barn, annenpart)
        );
    }

    private static List<PersonDto.BarnDto> tilBarn(List<PdlOppslagTjeneste.PersonMedIdent> barn, Map<String, PdlOppslagTjeneste.PersonMedIdent> annenpart) {
        return safeStream(barn)
                .map(barnet -> tilBarn(barnet, annenpart))
                .sorted(comparing(PersonDto.BarnDto::fødselsdato))
                .toList();
    }

    private static PersonDto.BarnDto tilBarn(PdlOppslagTjeneste.PersonMedIdent barnet, Map<String, PdlOppslagTjeneste.PersonMedIdent> annenpart) {
        if (barnet.ident() == null) { // Dødfødt barn
            return new PersonDto.BarnDto(
                    null,
                    fødselsdatoFor(barnet.person()),
                    dødsdatoFor(barnet.person()),
                    null,
                    null,
                    null
            );
        }

        return new PersonDto.BarnDto(
                new Fødselsnummer(barnet.ident()),
                fødselsdatoFor(barnet.person()),
                dødsdatoFor(barnet.person()),
                navnFor(barnet.person()),
                kjønnFor(barnet.person()),
                annenpart.containsKey(barnet.ident()) ? tilAnnenpart(annenpart.get(barnet.ident())) : null
        );
    }

    private static PersonDto.AnnenForelderDto tilAnnenpart(PdlOppslagTjeneste.PersonMedIdent personMedIdent) {
        var person = personMedIdent.person();
        return new PersonDto.AnnenForelderDto(
                new Fødselsnummer(personMedIdent.ident()),
                navnFor(person),
                fødselsdatoFor(person)
        );
    }


    private static Sivilstand tilSivilstand(Person søkerPerson) {
        return safeStream(søkerPerson.getSivilstand())
                .filter(Objects::nonNull)
                .map(PersonDtoMapper::tilSivilstand)
                .findFirst()
                .orElse(new Sivilstand(Sivilstand.Type.UOPPGITT));
    }

    private static Sivilstand tilSivilstand(no.nav.pdl.Sivilstand sivilstand) {
        var type = switch (sivilstand.getType()) {
            case UOPPGITT -> Sivilstand.Type.UOPPGITT;
            case UGIFT -> Sivilstand.Type.UGIFT;
            case GIFT -> Sivilstand.Type.GIFT;
            case ENKE_ELLER_ENKEMANN -> Sivilstand.Type.ENKE_ELLER_ENKEMANN;
            case SKILT -> Sivilstand.Type.SKILT;
            case SEPARERT -> Sivilstand.Type.SEPARERT;
            case REGISTRERT_PARTNER -> Sivilstand.Type.REGISTRERT_PARTNER;
            case SEPARERT_PARTNER -> Sivilstand.Type.SEPARERT_PARTNER;
            case SKILT_PARTNER -> Sivilstand.Type.SKILT_PARTNER;
            case GJENLEVENDE_PARTNER -> Sivilstand.Type.GJENLEVENDE_PARTNER;
            case null -> Sivilstand.Type.UOPPGITT;
        };
        return new Sivilstand(type);
    }

    private static Bankkonto tilBankkonto(KontonummerDto kontonummer) {
        if (KontonummerDto.UKJENT.equals(kontonummer)) {
            return Bankkonto.UKJENT;
        }
        return new Bankkonto(kontonummer.kontonummer(), tilBankNavn(kontonummer.utenlandskKontoInfo()));
    }

    private static String tilBankNavn(KontonummerDto.UtenlandskKontoInfo utenlandskKontoInfo) {
        if (utenlandskKontoInfo == null) {
            return null;
        }
        return utenlandskKontoInfo.banknavn();
    }

    private static Kjønn kjønnFor(Person person) {
        return safeStream(person.getKjoenn())
                .map(PersonDtoMapper::tilKjønn)
                .findFirst()
                .orElse(Kjønn.U);
    }

    private static Kjønn tilKjønn(Kjoenn kjoenn) {
        return switch (kjoenn.getKjoenn()) {
            case MANN -> Kjønn.M;
            case KVINNE -> Kjønn.K;
            case UKJENT -> Kjønn.U;
            case null -> Kjønn.U;
        };
    }


    private static no.nav.foreldrepenger.common.domain.Navn navnFor(Person person) {
        return safeStream(person.getNavn())
                .filter(Objects::nonNull)
                .map(PersonDtoMapper::navn)
                .findFirst()
                .orElse(null);
    }

    private static no.nav.foreldrepenger.common.domain.Navn navn(Navn navn) {
        return new no.nav.foreldrepenger.common.domain.Navn(
                navn.getFornavn(),
                navn.getMellomnavn(),
                navn.getEtternavn()
        );
    }
    private static LocalDate fødselsdatoFor(Person søkerPerson) {
        return safeStream(søkerPerson.getFoedselsdato())
                .map(Foedselsdato::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst()
                .map(PersonDtoMapper::tilLocalDato)
                .orElse(null);
    }

    private static LocalDate dødsdatoFor(Person person) {
        return safeStream(person.getDoedsfall())
                .map(Doedsfall::getDoedsdato)
                .filter(Objects::nonNull)
                .findFirst()
                .map(PersonDtoMapper::tilLocalDato)
                .orElse(null);
    }

    private static LocalDate tilLocalDato(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE); // FORMTER?
    }
}
