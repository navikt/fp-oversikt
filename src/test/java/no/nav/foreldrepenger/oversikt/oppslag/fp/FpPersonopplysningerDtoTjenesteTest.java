package no.nav.foreldrepenger.oversikt.oppslag.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.arbeid.Stillingsprosent;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlientSystem;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Kjønn;
import no.nav.foreldrepenger.oversikt.oppslag.felles.MineArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.DoedfoedtBarn;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Folkeregisteridentifikator;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonBolkResult;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.Sivilstandstype;

@ExtendWith(MockitoExtension.class)
class FpPersonopplysningerDtoTjenesteTest {

    private static final String BARN_1_IDENT = "11111111111";
    private static final String BARN_2_IDENT = "22222222222";
    private static final String ANNENPART_IDENT = "33333333333";

    @Mock
    private PdlKlient pdlKlient;

    @Mock
    private PdlKlientSystem pdlKlientSystem;

    @Mock
    private MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste;

    private FpPersonopplysningerDtoTjeneste tjeneste(DummyInnloggetTestbruker innloggetBruker) {
        return new FpPersonopplysningerDtoTjeneste(pdlKlient, pdlKlientSystem, mineArbeidsforholdTjeneste, innloggetBruker);
    }

    @Test
    void søker_happy_case_uten_barn_registrert() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.UGIFT);
        søkerPdl.setForelderBarnRelasjon(List.of());
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(innloggetBruker.fødselsnummer().value(), "I_BRUK", "FNR", null, null)));

        var arbeidsforhold = List.of(
            new EksternArbeidsforholdDto("123456789", "org", "Arbeidsgiver AS", Stillingsprosent.arbeid(BigDecimal.valueOf(100)),
                LocalDate.now().minusYears(5), null)
        );

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(arbeidsforhold);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto).isNotNull();
        assertThat(dto.fnr()).isEqualTo(innloggetBruker.fødselsnummer());
        assertThat(dto.navn().fornavn()).isEqualTo("Kari");
        assertThat(dto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(dto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));
        assertThat(dto.erGift()).isFalse();
        assertThat(dto.barn()).isEmpty();
        assertThat(dto.arbeidsforhold()).hasSize(1);
        assertThat(dto.arbeidsforhold().getFirst().arbeidsgiverNavn()).isEqualTo("Arbeidsgiver AS");
        assertThat(dto.arbeidsforhold().getFirst().stillingsprosent().prosent()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void happy_case_søker_med_ett_barn_og_annenpart_registrert() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.GIFT);
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(
            forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnPdl = lagBarn(LocalDate.now().minusMonths(3), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        );
        barnPdl.setNavn(List.of(navn("BARN", "BARNESEN", "DEN FØRSTE")));
        barnPdl.setKjoenn(List.of(new Kjoenn(KjoennType.KVINNE, null, null)));

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(List.of(navn("OLA", null, "NORDMANN")));
        annenpartPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(32)));
        annenpartPdl.setDoedsfall(List.of());
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        // Første bolk-kall: hent barn
        // Andre bolk-kall: hent annenpart
        when(pdlKlientSystem.hentPersonBolk(any(), any()))
            .thenReturn(List.of(new HentPersonBolkResult(BARN_1_IDENT, barnPdl, null)))
            .thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpartPdl, null)));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.fnr()).isEqualTo(innloggetBruker.fødselsnummer());
        assertThat(dto.navn().fornavn()).isEqualTo("Kari");
        assertThat(dto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(dto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));
        assertThat(dto.erGift()).isTrue();

        assertThat(dto.barn()).hasSize(1);
        var barnDto = dto.barn().getFirst();
        assertThat(barnDto.fnr().value()).isEqualTo(BARN_1_IDENT);
        assertThat(barnDto.navn().fornavn()).isEqualTo("Barn");
        assertThat(barnDto.navn().mellomnavn()).isEqualTo("Barnesen");
        assertThat(barnDto.navn().etternavn()).isEqualTo("Den Første");
        assertThat(barnDto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(barnDto.fødselsdato()).isEqualTo(LocalDate.now().minusMonths(3));

        var annenpartDto = barnDto.annenPart();
        assertThat(annenpartDto).isNotNull();
        assertThat(annenpartDto.fnr().value()).isEqualTo(ANNENPART_IDENT);
        assertThat(annenpartDto.navn().fornavn()).isEqualTo("Ola");
        assertThat(annenpartDto.navn().etternavn()).isEqualTo("Nordmann");
        assertThat(annenpartDto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(32));

        assertThat(dto.arbeidsforhold()).isEmpty();
    }

    private static Navn navn(String fornavn, String mellomnavn, String etternavn) {
        return new Navn(fornavn, mellomnavn, etternavn, null, null, null, null, null);
    }

    @Test
    void barn_som_er_eldre_enn_40mnd_skal_ikke_returneres() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.UGIFT);
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(
            forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
            forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnEldreEnn40Mnd = lagBarn(LocalDate.now().minusMonths(41), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN));
        barnEldreEnn40Mnd.setKjoenn(List.of(new Kjoenn(KjoennType.MANN, null, null)));
        barnEldreEnn40Mnd.setNavn(List.of(navn("GAMMEL", null, "BARN")));

        var barnYngreEnn40Mnd = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN));
        barnYngreEnn40Mnd.setKjoenn(List.of(new Kjoenn(KjoennType.KVINNE, null, null)));
        barnYngreEnn40Mnd.setNavn(List.of(navn("UNG", null, "BARN")));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(
            new HentPersonBolkResult(BARN_1_IDENT, barnEldreEnn40Mnd, null),
            new HentPersonBolkResult(BARN_2_IDENT, barnYngreEnn40Mnd, null)
        ));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        assertThat(dto.barn().getFirst().fnr().value()).isEqualTo(BARN_2_IDENT);
        assertThat(dto.barn().getFirst().fødselsdato()).isEqualTo(LocalDate.now().minusMonths(14));
    }

    @Test
    void barn_med_adressebeskyttelse_skal_ikke_returneres() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.UGIFT);
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(
            forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
            forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnBeskyttet = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.STRENGT_FORTROLIG);
        barnBeskyttet.setKjoenn(List.of(new Kjoenn(KjoennType.MANN, null, null)));
        barnBeskyttet.setNavn(List.of(navn("BESKYTTET", null, "BARN")));

        var barnUgradert = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.UGRADERT);
        barnUgradert.setKjoenn(List.of(new Kjoenn(KjoennType.KVINNE, null, null)));
        barnUgradert.setNavn(List.of(navn("UGRADERT", null, "BARN")));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(
            new HentPersonBolkResult(BARN_1_IDENT, barnBeskyttet, null),
            new HentPersonBolkResult(BARN_2_IDENT, barnUgradert, null)
        ));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        assertThat(dto.barn().getFirst().fnr().value()).isEqualTo(BARN_2_IDENT);
    }

    @Test
    void dødfødt_barn_skal_returneres_når_dødfødt_for_mindre_enn_40_mnd_siden() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.UGIFT);
        søkerPdl.setForelderBarnRelasjon(List.of());
        var dødfødtDatoNylig = LocalDate.now().minusWeeks(2);
        var dødfødtDatoGammel = LocalDate.now().minusYears(4);
        søkerPdl.setDoedfoedtBarn(List.of(
            new DoedfoedtBarn(tilStreng(dødfødtDatoNylig), null, null),
            new DoedfoedtBarn(tilStreng(dødfødtDatoGammel), null, null)
        ));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        var barnDto = dto.barn().getFirst();
        assertThat(barnDto.fnr()).isNull();
        assertThat(barnDto.fødselsdato()).isEqualTo(dødfødtDatoNylig);
        assertThat(barnDto.dødsdato()).isEqualTo(dødfødtDatoNylig);
    }

    @Test
    void annenpart_med_adressebeskyttelse_skal_ikke_returneres() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.UGIFT);
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(
            forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnPdl = lagBarn(LocalDate.now().minusMonths(3), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        );
        barnPdl.setNavn(List.of(navn("BARN", null, "NORDMANN")));
        barnPdl.setKjoenn(List.of(new Kjoenn(KjoennType.KVINNE, null, null)));

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(List.of(navn("OLA", null, "NORDMANN")));
        annenpartPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(32)));
        annenpartPdl.setDoedsfall(List.of());
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any()))
            .thenReturn(List.of(new HentPersonBolkResult(BARN_1_IDENT, barnPdl, null)))
            .thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpartPdl, null)));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        assertThat(dto.barn().getFirst().annenPart()).isNull();
    }

    @Test
    void annenpart_som_er_død_skal_ikke_returneres() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.UGIFT);
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(
            forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnPdl = lagBarn(LocalDate.now().minusMonths(3), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        );
        barnPdl.setNavn(List.of(navn("BARN", null, "NORDMANN")));
        barnPdl.setKjoenn(List.of(new Kjoenn(KjoennType.KVINNE, null, null)));

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(List.of(navn("OLA", null, "NORDMANN")));
        annenpartPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(32)));
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));
        annenpartPdl.setDoedsfall(List.of(new Doedsfall(tilStreng(LocalDate.now().minusMonths(1)), null, null)));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any()))
            .thenReturn(List.of(new HentPersonBolkResult(BARN_1_IDENT, barnPdl, null)))
            .thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpartPdl, null)));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        assertThat(dto.barn().getFirst().annenPart()).isNull();
    }

    @Test
    void søker_med_flere_barn_og_forskjellige_annenforeldre() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.UGIFT);
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(
            forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
            forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)
        ));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barn1Pdl = lagBarn(LocalDate.now().minusMonths(11), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        );
        barn1Pdl.setNavn(List.of(navn("BARN", null, "DEN FØRSTE")));
        barn1Pdl.setKjoenn(List.of(new Kjoenn(KjoennType.KVINNE, null, null)));

        var barn2Pdl = lagBarn(LocalDate.now().minusWeeks(2), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN)
            // Ingen annen forelder registrert på barn 2
        );
        barn2Pdl.setNavn(List.of(navn("BARN", null, "DEN ANDRE")));
        barn2Pdl.setKjoenn(List.of(new Kjoenn(KjoennType.KVINNE, null, null)));

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(List.of(navn("OLA", null, "NORDMANN")));
        annenpartPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(32)));
        annenpartPdl.setDoedsfall(List.of());
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any()))
            .thenReturn(List.of(
                new HentPersonBolkResult(BARN_1_IDENT, barn1Pdl, null),
                new HentPersonBolkResult(BARN_2_IDENT, barn2Pdl, null)
            ))
            .thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpartPdl, null)));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(2);

        // Barna er sortert etter fødselsdato, så barn1 (11 mnd) kommer først, barn2 (2 uker) sist
        var barn1Dto = dto.barn().getFirst();
        assertThat(barn1Dto.fnr().value()).isEqualTo(BARN_1_IDENT);
        assertThat(barn1Dto.fødselsdato()).isEqualTo(LocalDate.now().minusMonths(11));
        assertThat(barn1Dto.annenPart()).isNotNull();
        assertThat(barn1Dto.annenPart().fnr().value()).isEqualTo(ANNENPART_IDENT);
        assertThat(barn1Dto.annenPart().navn().fornavn()).isEqualTo("Ola");

        var barn2Dto = dto.barn().get(1);
        assertThat(barn2Dto.fnr().value()).isEqualTo(BARN_2_IDENT);
        assertThat(barn2Dto.fødselsdato()).isEqualTo(LocalDate.now().minusWeeks(2));
        assertThat(barn2Dto.annenPart()).isNull();
    }

    @Test
    void flere_arbeidsforhold_mappes_riktig() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28), Sivilstandstype.UGIFT);
        søkerPdl.setForelderBarnRelasjon(List.of());
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var arbeidsforhold1 = new EksternArbeidsforholdDto(
            "2", "org", "Arbeidsgiver 1", Stillingsprosent.arbeid(BigDecimal.valueOf(100)), LocalDate.now().minusYears(5), LocalDate.now()
        );
        var arbeidsforhold2 = new EksternArbeidsforholdDto(
            "12312312312", "org", "Arbeidsgiver 2", Stillingsprosent.arbeid(BigDecimal.valueOf(100)), LocalDate.now().minusYears(5), null
        );
        var arbeidsforhold3 = new EksternArbeidsforholdDto(
            "1", "fnr", "Arbeidsgiver 3", Stillingsprosent.arbeid(BigDecimal.valueOf(55)), LocalDate.now(), null
        );

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of(arbeidsforhold1, arbeidsforhold2, arbeidsforhold3));

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.arbeidsforhold()).hasSize(3);

        var a1 = dto.arbeidsforhold().getFirst();
        assertThat(a1.arbeidsgiverId()).isEqualTo("2");
        assertThat(a1.arbeidsgiverNavn()).isEqualTo("Arbeidsgiver 1");
        assertThat(a1.stillingsprosent().prosent()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(a1.fom()).isEqualTo(LocalDate.now().minusYears(5));
        assertThat(a1.tom()).isEqualTo(LocalDate.now());

        var a2 = dto.arbeidsforhold().get(1);
        assertThat(a2.arbeidsgiverId()).isEqualTo("12312312312");
        assertThat(a2.tom()).isNull();

        var a3 = dto.arbeidsforhold().get(2);
        assertThat(a3.arbeidsgiverId()).isEqualTo("1");
        assertThat(a3.stillingsprosent().prosent()).isEqualTo(BigDecimal.valueOf(55));
    }

    private static Person lagSøker(String fornavn, String mellomnavn, String etternavn, KjoennType kjønn, LocalDate fødselsdato, Sivilstandstype sivilstand) {
        var person = new Person();
        person.setNavn(List.of(navn(fornavn, mellomnavn, etternavn)));
        person.setKjoenn(List.of(new Kjoenn(kjønn, null, null)));
        person.setFoedselsdato(fødselsdato(fødselsdato));
        person.setSivilstand(List.of(new Sivilstand(sivilstand, null, null, null, null, null)));
        return person;
    }

    private static Person lagBarn(LocalDate fødselsdato, AdressebeskyttelseGradering gradering, ForelderBarnRelasjon... relasjoner) {
        var barn = new Person();
        barn.setFoedselsdato(fødselsdato(fødselsdato));
        barn.setAdressebeskyttelse(adressebeskyttelse(gradering));
        barn.setForelderBarnRelasjon(List.of(relasjoner));
        barn.setDoedsfall(List.of());
        return barn;
    }

    private static List<Foedselsdato> fødselsdato(LocalDate dato) {
        return List.of(new Foedselsdato(dato.format(DateTimeFormatter.ISO_LOCAL_DATE), null, null, null));
    }

    private static List<Adressebeskyttelse> adressebeskyttelse(AdressebeskyttelseGradering gradering) {
        return List.of(new Adressebeskyttelse(gradering, null, null));
    }

    private static ForelderBarnRelasjon forelderBarnRelasjon(String ident, ForelderBarnRelasjonRolle rolle, ForelderBarnRelasjonRolle minRolle) {
        return new ForelderBarnRelasjon(ident, rolle, minRolle, null, null, null);
    }

    private static String tilStreng(LocalDate dato) {
        return dato.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}

