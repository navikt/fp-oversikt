package no.nav.foreldrepenger.oversikt.oppslag;

import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.Sivilstandstype;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdlTestUtil {

    private PdlTestUtil() {
        // Utility class
    }

    static List<Adressebeskyttelse> adressebeskyttelse(AdressebeskyttelseGradering adressebeskyttelseGradering) {
        return List.of(new Adressebeskyttelse(adressebeskyttelseGradering, null, null));
    }

    static ForelderBarnRelasjon forelderBarnRelasjon(String ident, ForelderBarnRelasjonRolle relatertPersonRolle, ForelderBarnRelasjonRolle minRolleForPersonen) {
        return new ForelderBarnRelasjon(ident, relatertPersonRolle, minRolleForPersonen, null, null, null);
    }

    static List<Sivilstand> siviltilstand(Sivilstandstype sivilstandstype) {
        return List.of(new Sivilstand(sivilstandstype, null, null, null, null, null));
    }

    static List<Foedselsdato> fødselsdato(LocalDate localDate) {
        return List.of(new Foedselsdato(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE), null, null, null));
    }

    static List<Kjoenn> kjønn(KjoennType kjønn) {
        return List.of(new Kjoenn(kjønn, null, null));
    }

    static List<Navn> navn(String fornavn, String mellomnavn, String etternavn) {
        return List.of(new Navn(fornavn, mellomnavn, etternavn, null, null, null, null, null));
    }

    static Person lagBarn(ForelderBarnRelasjon... forelderBarnRelasjoner) {
        return lagBarn(LocalDate.now().minusMonths(10), AdressebeskyttelseGradering.UGRADERT, forelderBarnRelasjoner);
    }

    static Person lagBarn(LocalDate fødselsdato, AdressebeskyttelseGradering adressebeskyttelse, ForelderBarnRelasjon... forelderBarnRelasjoner) {
        var barnet = new Person();
        barnet.setFoedselsdato(List.of(new Foedselsdato(tilStreng(fødselsdato), null, null, null)));
        barnet.setAdressebeskyttelse(adressebeskyttelse(adressebeskyttelse));
        barnet.setForelderBarnRelasjon(List.of(forelderBarnRelasjoner));
        return barnet;
    }

    static String tilStreng(LocalDate localDate) {
        return localDate.format(DateTimeFormatter.ISO_DATE);
    }
}
