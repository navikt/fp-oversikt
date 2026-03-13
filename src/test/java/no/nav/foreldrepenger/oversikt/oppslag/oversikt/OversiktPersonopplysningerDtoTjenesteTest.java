package no.nav.foreldrepenger.oversikt.oppslag.oversikt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.oversikt.arbeid.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer.KontaktInformasjonKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer.KontonummerDto;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlientSystem;
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
import no.nav.pdl.Navn;
import no.nav.pdl.Person;

@ExtendWith(MockitoExtension.class)
class OversiktPersonopplysningerDtoTjenesteTest {

    private static final String BARN_1_IDENT = "11111111111";
    private static final String BARN_2_IDENT = "22222222222";
    private static final String ANNENPART_IDENT = "33333333333";

    @Mock
    private PdlKlient pdlKlient;

    @Mock
    private PdlKlientSystem pdlKlientSystem;

    @Mock
    private KontaktInformasjonKlient kontaktInformasjonKlient;

    @Mock
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    private OversiktPersonopplysningerDtoTjeneste tjeneste(DummyInnloggetTestbruker innloggetBruker) {
        return new OversiktPersonopplysningerDtoTjeneste(pdlKlientSystem, pdlKlient, kontaktInformasjonKlient, arbeidsforholdTjeneste, innloggetBruker);
    }

    @Test
    void søker_happy_case_uten_barn_registrert() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setForelderBarnRelasjon(List.of());
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setFolkeregisteridentifikator(
            List.of(new Folkeregisteridentifikator(innloggetBruker.fødselsnummer().value(), "I_BRUK", "FNR", null, null)));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(new KontonummerDto("12345678903", null));
        when(arbeidsforholdTjeneste.harArbeidsforhold(any())).thenReturn(true);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto).isNotNull();
        assertThat(dto.fnr()).isEqualTo(innloggetBruker.fødselsnummer());
        assertThat(dto.navn().fornavn()).isEqualTo("Kari");
        assertThat(dto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));
        assertThat(dto.kontonummer()).isEqualTo("12345678903");
        assertThat(dto.harArbeidsforhold()).isTrue();
        assertThat(dto.barn()).isEmpty();
    }

    @Test
    void søker_med_ukjent_kontonummer_gir_null() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setForelderBarnRelasjon(List.of());
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setFolkeregisteridentifikator(
            List.of(new Folkeregisteridentifikator(innloggetBruker.fødselsnummer().value(), "I_BRUK", "FNR", null, null)));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.kontonummer()).isNull();
    }

    @Test
    void søker_uten_arbeidsforhold_gir_harArbeidsforhold_false() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setForelderBarnRelasjon(List.of());
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setFolkeregisteridentifikator(
            List.of(new Folkeregisteridentifikator(innloggetBruker.fødselsnummer().value(), "I_BRUK", "FNR", null, null)));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);
        when(arbeidsforholdTjeneste.harArbeidsforhold(any())).thenReturn(false);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.harArbeidsforhold()).isFalse();
    }

    @Test
    void happy_case_søker_med_ett_barn_og_annenpart_registrert() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnPdl = lagBarn(LocalDate.now().minusMonths(3), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN));

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(List.of(navn("OLA", null, "NORDMANN")));
        annenpartPdl.setDoedsfall(List.of());
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(new HentPersonBolkResult(BARN_1_IDENT, barnPdl, null)))
            .thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpartPdl, null)));
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.fnr()).isEqualTo(innloggetBruker.fødselsnummer());
        assertThat(dto.navn().fornavn()).isEqualTo("Kari");
        assertThat(dto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));

        assertThat(dto.barn()).hasSize(1);
        var barnDto = dto.barn().getFirst();
        assertThat(barnDto.fnr().value()).isEqualTo(BARN_1_IDENT);
        assertThat(barnDto.fødselsdato()).isEqualTo(LocalDate.now().minusMonths(3));
        assertThat(barnDto.dødsdato()).isNull();
        assertThat(barnDto.navn().fornavn()).isEqualTo("Barn");
        assertThat(barnDto.annenPartFornavn()).isEqualTo("Ola");
    }

    @Test
    void barn_som_er_eldre_enn_40mnd_skal_ikke_returneres() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
            forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnEldreEnn40Mnd = lagBarn(LocalDate.now().minusMonths(41), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN));

        var barnYngreEnn40Mnd = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(new HentPersonBolkResult(BARN_1_IDENT, barnEldreEnn40Mnd, null),
            new HentPersonBolkResult(BARN_2_IDENT, barnYngreEnn40Mnd, null)));
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);

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

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
            forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnBeskyttet = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.STRENGT_FORTROLIG);

        var barnUgradert = lagBarn(LocalDate.now().minusMonths(14), AdressebeskyttelseGradering.UGRADERT);

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(
            List.of(new HentPersonBolkResult(BARN_1_IDENT, barnBeskyttet, null), new HentPersonBolkResult(BARN_2_IDENT, barnUgradert, null)));
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        assertThat(dto.barn().getFirst().fnr().value()).isEqualTo(BARN_2_IDENT);
    }

    @Test
    void dødfødt_barn_skal_returneres_når_dødfødt_for_mindre_enn_40_mnd_siden() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setForelderBarnRelasjon(List.of());
        var dødfødtDatoNylig = LocalDate.now().minusWeeks(2);
        var dødfødtDatoGammel = LocalDate.now().minusYears(4);
        søkerPdl.setDoedfoedtBarn(
            List.of(new DoedfoedtBarn(tilStreng(dødfødtDatoNylig), null, null), new DoedfoedtBarn(tilStreng(dødfødtDatoGammel), null, null)));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        var barnDto = dto.barn().getFirst();
        assertThat(barnDto.fnr()).isNull();
        assertThat(barnDto.fødselsdato()).isEqualTo(dødfødtDatoNylig);
        assertThat(barnDto.dødsdato()).isEqualTo(dødfødtDatoNylig);
        assertThat(barnDto.navn()).isNull();
    }

    @Test
    void annenpart_med_adressebeskyttelse_skal_ikke_returneres() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnPdl = lagBarn(LocalDate.now().minusMonths(3), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN));

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(List.of(navn("OLA", null, "NORDMANN")));
        annenpartPdl.setDoedsfall(List.of());
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(new HentPersonBolkResult(BARN_1_IDENT, barnPdl, null)))
            .thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpartPdl, null)));
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        assertThat(dto.barn().getFirst().annenPartFornavn()).isNull();
    }

    @Test
    void annenpart_som_er_død_skal_ikke_returneres() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barnPdl = lagBarn(LocalDate.now().minusMonths(3), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN));

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(List.of(navn("OLA", null, "NORDMANN")));
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));
        annenpartPdl.setDoedsfall(List.of(new Doedsfall(tilStreng(LocalDate.now().minusMonths(1)), null, null)));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(List.of(new HentPersonBolkResult(BARN_1_IDENT, barnPdl, null)))
            .thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpartPdl, null)));
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(1);
        assertThat(dto.barn().getFirst().annenPartFornavn()).isNull();
    }

    @Test
    void søker_med_flere_barn_og_forskjellige_annenforeldre() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);
        var søkersIdent = innloggetBruker.fødselsnummer().value();

        var søkerPdl = lagSøker("KARI", null, "KANARI", LocalDate.now().minusYears(28));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR),
            forelderBarnRelasjon(BARN_2_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(søkersIdent, "I_BRUK", "FNR", null, null)));

        var barn1Pdl = lagBarn(LocalDate.now().minusMonths(11), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
            forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN));

        var barn2Pdl = lagBarn(LocalDate.now().minusWeeks(2), AdressebeskyttelseGradering.UGRADERT,
            forelderBarnRelasjon(søkersIdent, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN)
            // Ingen annen forelder registrert på barn 2
        );

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(List.of(navn("OLA", null, "NORDMANN")));
        annenpartPdl.setDoedsfall(List.of());
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(pdlKlientSystem.hentPersonBolk(any(), any())).thenReturn(
                List.of(new HentPersonBolkResult(BARN_1_IDENT, barn1Pdl, null), new HentPersonBolkResult(BARN_2_IDENT, barn2Pdl, null)))
            .thenReturn(List.of(new HentPersonBolkResult(ANNENPART_IDENT, annenpartPdl, null)));
        when(kontaktInformasjonKlient.hentRegistertKontonummerMedFallback()).thenReturn(KontonummerDto.UKJENT);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto.barn()).hasSize(2);

        // Barna er sortert etter fødselsdato, så barn1 (11 mnd) kommer først, barn2 (2 uker) sist
        var barn1Dto = dto.barn().getFirst();
        assertThat(barn1Dto.fnr().value()).isEqualTo(BARN_1_IDENT);
        assertThat(barn1Dto.fødselsdato()).isEqualTo(LocalDate.now().minusMonths(11));
        assertThat(barn1Dto.annenPartFornavn()).isEqualTo("Ola");

        var barn2Dto = dto.barn().get(1);
        assertThat(barn2Dto.fnr().value()).isEqualTo(BARN_2_IDENT);
        assertThat(barn2Dto.fødselsdato()).isEqualTo(LocalDate.now().minusWeeks(2));
        assertThat(barn2Dto.annenPartFornavn()).isNull();
    }

    private static Navn navn(String fornavn, String mellomnavn, String etternavn) {
        return new Navn(fornavn, mellomnavn, etternavn, null, null, null, null, null);
    }

    private static Person lagSøker(String fornavn, String mellomnavn, String etternavn, LocalDate fødselsdato) {
        var person = new Person();
        person.setNavn(List.of(navn(fornavn, mellomnavn, etternavn)));
        person.setFoedselsdato(fødselsdato(fødselsdato));
        return person;
    }

    private static Person lagBarn(LocalDate fødselsdato, AdressebeskyttelseGradering gradering, ForelderBarnRelasjon... relasjoner) {
        var barn = new Person();
        barn.setNavn(List.of(navn("BARN", null, "BARNESEN")));
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
