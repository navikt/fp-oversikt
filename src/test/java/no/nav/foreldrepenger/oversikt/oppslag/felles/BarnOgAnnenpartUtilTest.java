package no.nav.foreldrepenger.oversikt.oppslag.felles;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.DoedfoedtBarn;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.Person;

class BarnOgAnnenpartUtilTest {

    private static final String SØKER_IDENT = "12345678901";
    private static final String BARN_IDENT = "11111111111";
    private static final String ANNENPART_IDENT = "22222222222";

    @Test
    void dødfødtBarn_skal_sette_fødselsdato_og_dødsdato_lik_dato_fra_pdl() {
        var dato = "2024-03-15";
        var df = new DoedfoedtBarn(dato, null, null);

        var person = BarnOgAnnenpartUtil.dødfødtBarn(df);

        assertThat(person.getFoedselsdato()).hasSize(1);
        assertThat(person.getFoedselsdato().getFirst().getFoedselsdato()).isEqualTo(dato);
        assertThat(person.getDoedsfall()).hasSize(1);
        assertThat(person.getDoedsfall().getFirst().getDoedsdato()).isEqualTo(dato);
    }

    @Test
    void barnRelatertTil_returnerer_barn_identer() {
        var person = new Person();
        person.setForelderBarnRelasjon(List.of(
            forelderBarnRelasjon(BARN_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));

        var result = BarnOgAnnenpartUtil.barnRelatertTil(new PersonMedIdent(SØKER_IDENT, person));

        assertThat(result).containsExactly(BARN_IDENT);
    }

    @Test
    void barnRelatertTil_filtrerer_bort_null_identer() {
        var person = new Person();
        person.setForelderBarnRelasjon(List.of(
            forelderBarnRelasjon(BARN_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
            forelderBarnRelasjon(null, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));

        var result = BarnOgAnnenpartUtil.barnRelatertTil(new PersonMedIdent(SØKER_IDENT, person));

        assertThat(result).containsExactly(BARN_IDENT);
    }

    @Test
    void barnRelatertTil_returnerer_tom_liste_når_ingen_relasjoner() {
        var person = new Person();
        person.setForelderBarnRelasjon(List.of());

        var result = BarnOgAnnenpartUtil.barnRelatertTil(new PersonMedIdent(SØKER_IDENT, person));

        assertThat(result).isEmpty();
    }

    @Test
    void annenForelderRegisterertPåBarnet_finner_annen_forelder() {
        var barn = lagBarnMedRelasjoner(
            forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        );

        var result = BarnOgAnnenpartUtil.annenForelderRegisterertPåBarnet(new Fødselsnummer(SØKER_IDENT), barn);

        assertThat(result).contains(ANNENPART_IDENT);
    }

    @Test
    void annenForelderRegisterertPåBarnet_returnerer_empty_når_ingen_annen_forelder() {
        var barn = lagBarnMedRelasjoner(
            forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN)
        );

        var result = BarnOgAnnenpartUtil.annenForelderRegisterertPåBarnet(new Fødselsnummer(SØKER_IDENT), barn);

        assertThat(result).isEmpty();
    }

    @Test
    void annenForelderRegisterertPåBarnet_ekskluderer_søker_selv() {
        var barn = lagBarnMedRelasjoner(
            forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        );

        var result = BarnOgAnnenpartUtil.annenForelderRegisterertPåBarnet(new Fødselsnummer(SØKER_IDENT), barn);

        assertThat(result).isEmpty();
    }

    @Test
    void barnErYngreEnn40Mnd_returnerer_true_for_ungt_barn() {
        var barn = lagBarnMedFødselsdato(LocalDate.now().minusMonths(10));

        assertThat(BarnOgAnnenpartUtil.barnErYngreEnn40Mnd(barn)).isTrue();
    }

    @Test
    void barnErYngreEnn40Mnd_returnerer_false_for_gammelt_barn() {
        var barn = lagBarnMedFødselsdato(LocalDate.now().minusMonths(41));

        assertThat(BarnOgAnnenpartUtil.barnErYngreEnn40Mnd(barn)).isFalse();
    }

    @Test
    void harAdressebeskyttelse_returnerer_true_for_strengt_fortrolig() {
        var person = new Person();
        person.setAdressebeskyttelse(List.of(new Adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG, null, null)));

        assertThat(BarnOgAnnenpartUtil.harAdressebeskyttelse(person)).isTrue();
    }

    @Test
    void harAdressebeskyttelse_returnerer_true_for_fortrolig() {
        var person = new Person();
        person.setAdressebeskyttelse(List.of(new Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, null, null)));

        assertThat(BarnOgAnnenpartUtil.harAdressebeskyttelse(person)).isTrue();
    }

    @Test
    void harAdressebeskyttelse_returnerer_false_for_ugradert() {
        var person = new Person();
        person.setAdressebeskyttelse(List.of(new Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT, null, null)));

        assertThat(BarnOgAnnenpartUtil.harAdressebeskyttelse(person)).isFalse();
    }

    @Test
    void harDødsdato_returnerer_true_når_dødsdato_er_satt() {
        var person = new Person();
        person.setDoedsfall(List.of(new Doedsfall("2024-01-01", null, null)));

        assertThat(BarnOgAnnenpartUtil.harDødsdato(person)).isTrue();
    }

    @Test
    void harDødsdato_returnerer_false_for_tom_liste() {
        var person = new Person();
        person.setDoedsfall(List.of());

        assertThat(BarnOgAnnenpartUtil.harDødsdato(person)).isFalse();
    }

    @Test
    void harDødsdato_returnerer_false_for_null() {
        var person = new Person();
        person.setDoedsfall(null);

        assertThat(BarnOgAnnenpartUtil.harDødsdato(person)).isFalse();
    }

    private static PersonMedIdent lagBarnMedRelasjoner(ForelderBarnRelasjon... relasjoner) {
        var person = new Person();
        person.setForelderBarnRelasjon(List.of(relasjoner));
        return new PersonMedIdent(BARN_IDENT, person);
    }

    private static PersonMedIdent lagBarnMedFødselsdato(LocalDate fødselsdato) {
        var person = new Person();
        person.setFoedselsdato(List.of(new Foedselsdato(fødselsdato.format(DateTimeFormatter.ISO_LOCAL_DATE), null, null, null)));
        return new PersonMedIdent(BARN_IDENT, person);
    }

    private static ForelderBarnRelasjon forelderBarnRelasjon(String ident, ForelderBarnRelasjonRolle rolle, ForelderBarnRelasjonRolle minRolle) {
        return new ForelderBarnRelasjon(ident, rolle, minRolle, null, null, null);
    }
}

