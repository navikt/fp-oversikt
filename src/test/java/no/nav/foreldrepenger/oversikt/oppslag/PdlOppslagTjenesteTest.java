package no.nav.foreldrepenger.oversikt.oppslag;

import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlientSystem;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.DoedfoedtBarn;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonBolkResult;
import no.nav.pdl.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.adressebeskyttelse;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.forelderBarnRelasjon;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.fødselsdato;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.lagBarn;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.tilStreng;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdlOppslagTjenesteTest {
    private static final String SØKER_IDENT = "12345678901";
    private static final String BARN_1_IDENT = "111111";
    private static final String BARN_2_IDENT = "222222";
    private static final String ANNENPART_IDENT = "987654321";
    private static final String ANNENPART_2_IDENT = "393993939";

    @Mock
    private PdlKlient pdlKlient;

    @Mock
    private PdlKlientSystem pdlKlientSystem;

    @InjectMocks
    private PdlOppslagTjeneste pdlOppslagTjeneste;

    @Test
    void skal_returnere_søker_selv_om_en_selv_har_adressebeskyttelse() {
        var person = new Person();
        person.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG));
        when(pdlKlient.hentPerson(any(), any())).thenReturn(person);

        var result = pdlOppslagTjeneste.hentSøker(SØKER_IDENT);

        assertThat(result.ident()).isEqualTo(SØKER_IDENT);
        assertThat(result.person().getAdressebeskyttelse()).hasSize(1);
        assertThat(result.person().getAdressebeskyttelse().getFirst().getGradering()).isEqualTo(AdressebeskyttelseGradering.STRENGT_FORTROLIG);
    }

    @Test
    void forelder_har_ingen_relaterter_barn_og_skal_derfor_ikke_returnere_noen_barn() {
        var søker = new Person();

        var result = pdlOppslagTjeneste.hentBarnTilSøker(new PdlOppslagTjeneste.PersonMedIdent("12345678901", søker));

        assertThat(result).isEmpty();
    }

    @Test
    void barn_som_er_eldre_enn_40mnd_skal_ikke_returneres() {
        var søker = new Person();
        søker.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
                forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        var barnEldreEnn40Mnd = lagBarn(LocalDate.now().minusMonths(41), AdressebeskyttelseGradering.UGRADERT,
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN));

        var barnYngreEnn40Mnd = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.UGRADERT,
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN));


        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(
                new HentPersonBolkResult(BARN_1_IDENT, barnEldreEnn40Mnd, null),
                new HentPersonBolkResult(BARN_2_IDENT, barnYngreEnn40Mnd, null)
        ));

        var result = pdlOppslagTjeneste.hentBarnTilSøker(new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søker));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().ident()).isEqualTo(BARN_2_IDENT);
        assertThat(result.getFirst().person().getFoedselsdato().getFirst().getFoedselsdato()).isEqualTo(barnYngreEnn40Mnd.getFoedselsdato().getFirst().getFoedselsdato());
    }

    @Test
    void barn_som_har_adressebeskyttelse_skal_ikke_returneres() {
        var barnAdressebeskyttelse = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.STRENGT_FORTROLIG);
        var barnUgradert = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.UGRADERT);
        var søker = new Person();
        søker.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
                forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));


        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(
                new HentPersonBolkResult(BARN_1_IDENT, barnAdressebeskyttelse, null),
                new HentPersonBolkResult(BARN_2_IDENT, barnUgradert, null)
        ));

        var result = pdlOppslagTjeneste.hentBarnTilSøker(new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søker));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().ident()).isEqualTo(BARN_2_IDENT);
        assertThat(result.getFirst().person().getFoedselsdato().getFirst().getFoedselsdato()).isEqualTo(barnUgradert.getFoedselsdato().getFirst().getFoedselsdato());
    }

    @Test
    void dødfødte_barn_skal_også_returneres_så_lenge_det_er_dødfødt_for_mindre_enn_40_uker_siden() {
        var barnUgradert = lagBarn(LocalDate.now().minusMonths(10), AdressebeskyttelseGradering.UGRADERT);
        var søker = new Person();
        søker.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        var dødfødtDatoMindreEnn40UkerSiden = LocalDate.now().minusWeeks(2);
        var dødfødtDatoMerEnn40UkerSiden = LocalDate.now().minusYears(4);
        søker.setDoedfoedtBarn(List.of(
                new DoedfoedtBarn(tilStreng(dødfødtDatoMindreEnn40UkerSiden), null, null),
                new DoedfoedtBarn(tilStreng(dødfødtDatoMerEnn40UkerSiden), null, null)) // Skal ikke returneres
        );

        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(
                new HentPersonBolkResult(BARN_1_IDENT, barnUgradert, null)
        ));

        var result = pdlOppslagTjeneste.hentBarnTilSøker(new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søker));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).ident()).isEqualTo(BARN_1_IDENT);
        assertThat(result.get(0).person().getFoedselsdato().getFirst().getFoedselsdato()).isEqualTo(barnUgradert.getFoedselsdato().getFirst().getFoedselsdato());
        assertThat(result.get(1).ident()).isNull();
        assertThat(result.get(1).person().getFoedselsdato().getFirst().getFoedselsdato()).isEqualTo(tilStreng(dødfødtDatoMindreEnn40UkerSiden));
        assertThat(result.get(1).person().getDoedsfall().getFirst().getDoedsdato()).isEqualTo(tilStreng(dødfødtDatoMindreEnn40UkerSiden));
    }

    @Test
    void annnepart_happy_case() {
        var søker = new Person();
        søker.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        var annenpart = new Person();
        annenpart.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(30)));
        annenpart.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.FAR)));
        var barn =  lagBarn(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        );
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpart, null)));


        var result = pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(List.of(new PdlOppslagTjeneste.PersonMedIdent(BARN_1_IDENT, barn)), new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søker));

        assertThat(result).hasSize(1)
                .containsKey(BARN_1_IDENT);
        assertThat(result.get(BARN_1_IDENT).ident()).isEqualTo(ANNENPART_IDENT);
        assertThat(result.get(BARN_1_IDENT).person().getFoedselsdato().getFirst().getFoedselsdato()).isEqualTo(tilStreng(LocalDate.now().minusYears(30)));
    }

    @Test
    void annenpart_har_beskyttet_addresse_og_skal_ikke_hentes() {
        var søker = new Person();
        søker.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        var annenpart = new Person();
        annenpart.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.FAR)));
        annenpart.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG));
        var barn = lagBarn();
        barn.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpart, null)));

        var result = pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(List.of(
                new PdlOppslagTjeneste.PersonMedIdent(BARN_1_IDENT, barn)),
                new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søker));

        assertThat(result).isEmpty();
    }

    @Test
    void tvillinger_har_samme_annnepart_og_skal_bare_slå_opp_en_gang() {
        var søker = new Person();
        søker.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
                forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        var annenpart = new Person();
        annenpart.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MEDMOR),
                forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MEDMOR)
        ));

        var barn1 = lagBarn();
        barn1.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));
        var barn2 = lagBarn();
        barn2.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));

        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(
                new HentPersonBolkResult(ANNENPART_IDENT, annenpart, null)
        ));

        var result = pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(List.of(
                        new PdlOppslagTjeneste.PersonMedIdent(BARN_1_IDENT, barn1),
                        new PdlOppslagTjeneste.PersonMedIdent(BARN_2_IDENT, barn2)),
                new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søker));

        assertThat(result).hasSize(2)
                .containsKey(BARN_1_IDENT)
                .containsKey(BARN_2_IDENT);
    }

    @Test
    void to_barn_med_to_forskjellige_foreldre_en_har_beskyttet_adresse_mens_andre_ikke_har_det_skal_returene_bare_sistnevnte() {
        var søker = new Person();
        søker.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
                forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        var annenpartGradert = new Person();
        annenpartGradert.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MEDMOR)));
        annenpartGradert.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG));
        var annenpartUgradert = new Person();
        annenpartUgradert.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MEDMOR)));
        annenpartUgradert.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));

        var barnGradertAnnenforelder = lagBarn();
        barnGradertAnnenforelder.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));
        var barnUgradertAnnenforelder = lagBarn();
        barnUgradertAnnenforelder.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_2_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));

        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(
                        new HentPersonBolkResult(ANNENPART_IDENT, annenpartGradert, null),
                        new HentPersonBolkResult(ANNENPART_2_IDENT, annenpartUgradert, null)
                ));

        var result = pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(List.of(
                new PdlOppslagTjeneste.PersonMedIdent(BARN_1_IDENT, barnGradertAnnenforelder),
                new PdlOppslagTjeneste.PersonMedIdent(BARN_2_IDENT, barnUgradertAnnenforelder)),
                new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søker));

        assertThat(result).hasSize(1).containsKey(BARN_2_IDENT);
    }

    @Test
    void annenpart_er_registert_med_dato_for_død_og_skal_ikke_returneres() {
        var søker = new Person();
        søker.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        var annenpart = new Person();
        annenpart.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MEDMOR)));
        annenpart.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));
        annenpart.setDoedsfall(List.of(new Doedsfall(tilStreng(LocalDate.now().minusMonths(1)), null, null)));
        var barn = lagBarn();
        barn.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpart, null)));

        var result = pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(
                List.of(new PdlOppslagTjeneste.PersonMedIdent(BARN_1_IDENT, barn)),
                new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søker));

        assertThat(result).isEmpty();
    }

}