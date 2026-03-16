package no.nav.foreldrepenger.oversikt.oppslag.felles;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlientSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.DoedfoedtBarn;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.Person;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;

public class BarnOgAnnenpartUtil {

    private static final int IKKE_ELDRE_ENN_40_MND_BARN = 40;

    private BarnOgAnnenpartUtil() {
        // hide public constructor
    }

    public static Person dødfødtBarn(DoedfoedtBarn df) {
        var dødfødtBarn = new Person();
        dødfødtBarn.setFoedselsdato(List.of(new Foedselsdato(df.getDato(), null, null, null)));
        dødfødtBarn.setDoedsfall(List.of(new Doedsfall(df.getDato(), null, null)));
        return dødfødtBarn;
    }

    public static List<String> barnRelatertTil(PersonMedIdent person) {
        return Stream.ofNullable(person.person().getForelderBarnRelasjon())
            .flatMap(Collection::stream)
            .filter(r -> r.getRelatertPersonsRolle().equals(ForelderBarnRelasjonRolle.BARN))
            .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
            .filter(Objects::nonNull)
            .toList();
    }

    public static Optional<String> annenForelderRegisterertPåBarnet(Fødselsnummer søkersFnr, PersonMedIdent barnet) {
        return Stream.ofNullable(barnet.person().getForelderBarnRelasjon())
            .flatMap(Collection::stream)
            .filter(r -> !r.getRelatertPersonsRolle().equals(ForelderBarnRelasjonRolle.BARN))
            .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
            .filter(Objects::nonNull)
            .filter(relatertIdent -> !relatertIdent.equals(søkersFnr.value()))
            .findFirst();
    }

    public static boolean barnErYngreEnn40Mnd(PersonMedIdent barnet) {
        var fødselsdato = PersonMappers.mapFødselsdato(barnet.person()).orElseThrow();
        return fødselsdato.isAfter(LocalDate.now().minusMonths(IKKE_ELDRE_ENN_40_MND_BARN));
    }

    public static boolean harAdressebeskyttelse(Person person) {
        var graderinger = Stream.ofNullable(person.getAdressebeskyttelse())
            .flatMap(Collection::stream)
            .map(Adressebeskyttelse::getGradering)
            .map(PdlKlientSystem::tilGradering)
            .collect(Collectors.toSet());
        return new AdresseBeskyttelse(graderinger).harBeskyttetAdresse();
    }

    public static boolean harDødsdato(Person person) {
        return person.getDoedsfall() != null && !person.getDoedsfall().isEmpty();
    }
}

