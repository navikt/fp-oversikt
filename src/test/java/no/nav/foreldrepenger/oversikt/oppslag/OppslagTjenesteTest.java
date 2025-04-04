package no.nav.foreldrepenger.oversikt.oppslag;


import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.domain.felles.Kjønn;
import no.nav.foreldrepenger.common.oppslag.dkif.Målform;
import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.arbeid.Stillingsprosent;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.integrasjoner.digdir.KrrSpråkKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer.KontaktInformasjonKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer.KontonummerDto;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Person;
import no.nav.pdl.Sivilstandstype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.foreldrepenger.oversikt.oppslag.OppslagTjeneste.PERSONINFO_CACHE;
import static no.nav.foreldrepenger.oversikt.oppslag.OppslagTjeneste.PERSON_ARBEIDSFORHOLD_CACHE;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.adressebeskyttelse;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.forelderBarnRelasjon;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.fødselsdato;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.kjønn;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.navn;
import static no.nav.foreldrepenger.oversikt.oppslag.PdlTestUtil.siviltilstand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OppslagTjenesteTest {

    private static final String SØKER_IDENT = "12345678901";
    private static final String BARN_1_IDENT = "111111";
    private static final String BARN_2_IDENT = "222222";
    private static final String ANNENPART_IDENT = "987654321";

    @Mock
    private PdlOppslagTjeneste pdlOppslagTjeneste;

    @Mock
    private MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste;

    @Mock
    private KrrSpråkKlient krrSpråkKlient;

    @Mock
    private KontaktInformasjonKlient kontaktInformasjonKlient;

    @Mock
    private InnloggetBruker innloggetBruker;

    @InjectMocks
    private OppslagTjeneste oppslagTjeneste;

    @BeforeEach
    void setUp() {
        PERSONINFO_CACHE.remove(SØKER_IDENT);
        PERSON_ARBEIDSFORHOLD_CACHE.remove(SØKER_IDENT);
        when(innloggetBruker.fødselsnummer()).thenReturn(new Fødselsnummer("12345678901"));
        when(innloggetBruker.aktørId()).thenReturn(new AktørId("00012345678901"));
    }

    @Test
    void søker_happy_case_uten_barn_registert() {
        var søkerPdl = new Person();
        søkerPdl.setNavn(navn("Kari", null, "Kanari"));
        søkerPdl.setKjoenn(kjønn(KjoennType.KVINNE));
        søkerPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(28)));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setSivilstand(siviltilstand(Sivilstandstype.UGIFT));
        søkerPdl.setForelderBarnRelasjon(List.of());

        var enkeltArbeidsforhold = new EksternArbeidsforholdDto(
                "123456789",
                "fnr",
                "Navn på arbeidsgiver", Stillingsprosent.arbeid(BigDecimal.valueOf(100)), LocalDate.now().minusYears(5),
                Optional.empty()
        );

        when(pdlOppslagTjeneste.hentSøker(any())).thenReturn(new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søkerPdl));
        when(pdlOppslagTjeneste.hentBarnTilSøker(any())).thenReturn(List.of());
        when(pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(any(), any())).thenReturn(Map.of());
        when(krrSpråkKlient.finnSpråkkodeForBruker(any())).thenReturn(Målform.NB);
        when(kontaktInformasjonKlient.hentRegistertKontonummer()).thenReturn(new KontonummerDto("123456789", null));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of(enkeltArbeidsforhold));

        // Act
        var søkerinfo = oppslagTjeneste.personinfoMedArbeidsforholdFor();

        // Assert
        assertThat(søkerinfo).isNotNull();
        var søkerDto = søkerinfo.person();
        assertThat(søkerDto.navn().fornavn()).isEqualTo("Kari");
        assertThat(søkerDto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(søkerDto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(søkerDto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));
        assertThat(søkerDto.sivilstand()).isEqualTo(new no.nav.foreldrepenger.common.domain.felles.Sivilstand(no.nav.foreldrepenger.common.domain.felles.Sivilstand.Type.UGIFT));
        assertThat(søkerDto.målform()).isEqualTo(Målform.NB);
        assertThat(søkerDto.bankkonto().kontonummer()).isEqualTo("123456789");
        assertThat(søkerDto.barn()).isEmpty();

        assertThat(søkerinfo.arbeidsforhold()).hasSize(1);
        var arbeidsforholdDto = søkerinfo.arbeidsforhold().getFirst();
        assertThat(arbeidsforholdDto.arbeidsgiverId()).isEqualTo("123456789");
        assertThat(arbeidsforholdDto.arbeidsgiverNavn()).isEqualTo("Navn på arbeidsgiver");
        assertThat(arbeidsforholdDto.stillingsprosent().prosent()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(arbeidsforholdDto.from()).isEqualTo(LocalDate.now().minusYears(5));
        assertThat(arbeidsforholdDto.to()).isEmpty();
    }

    @Test
    void happy_case_søker_med_ett_barn_og_annenpart_registert() {
        var søkerPdl = new Person();
        søkerPdl.setNavn(navn("Kari", null, "Kanari"));
        søkerPdl.setKjoenn(kjønn(KjoennType.KVINNE));
        søkerPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(28)));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setSivilstand(siviltilstand(Sivilstandstype.UGIFT));
        søkerPdl.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));

        var annenpartPdl = new Person();
        annenpartPdl.setNavn(navn("Ola", null, "Nordmann"));
        annenpartPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(32)));
        annenpartPdl.setDoedsfall(List.of());
        annenpartPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));

        var barnPdl = new Person();
        barnPdl.setNavn(navn("Barn", "Barnesen", "Den første"));
        barnPdl.setKjoenn(kjønn(KjoennType.KVINNE));
        barnPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusMonths(3)));
        barnPdl.setDoedsfall(List.of());
        barnPdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));
        barnPdl.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));

        when(pdlOppslagTjeneste.hentSøker(any())).thenReturn(new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søkerPdl));
        when(pdlOppslagTjeneste.hentBarnTilSøker(any())).thenReturn(List.of(new PdlOppslagTjeneste.PersonMedIdent(BARN_1_IDENT, barnPdl)));
        when(pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(any(), any())).thenReturn(Map.of(BARN_1_IDENT, new PdlOppslagTjeneste.PersonMedIdent(ANNENPART_IDENT, annenpartPdl)));
        when(krrSpråkKlient.finnSpråkkodeForBruker(any())).thenReturn(Målform.NB);
        when(kontaktInformasjonKlient.hentRegistertKontonummer()).thenReturn(new KontonummerDto("123456789", null));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        // Act
        var søkerinfo = oppslagTjeneste.personinfoMedArbeidsforholdFor();

        // Asserts

        var søkerDto = søkerinfo.person();
        assertThat(søkerDto.navn().fornavn()).isEqualTo("Kari");
        assertThat(søkerDto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(søkerDto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(søkerDto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));
        assertThat(søkerDto.sivilstand()).isEqualTo(new no.nav.foreldrepenger.common.domain.felles.Sivilstand(no.nav.foreldrepenger.common.domain.felles.Sivilstand.Type.UGIFT));
        assertThat(søkerDto.målform()).isEqualTo(Målform.NB);
        assertThat(søkerDto.bankkonto().kontonummer()).isEqualTo("123456789");
        assertThat(søkerDto.barn()).hasSize(1);
        var barnDto = søkerDto.barn().getFirst();
        assertThat(barnDto.navn().fornavn()).isEqualTo("Barn");
        assertThat(barnDto.navn().mellomnavn()).isEqualTo("Barnesen");
        assertThat(barnDto.navn().etternavn()).isEqualTo("Den Første");
        assertThat(barnDto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(barnDto.fødselsdato()).isEqualTo(LocalDate.now().minusMonths(3));
        var annenpartDto = barnDto.annenPart();
        assertThat(annenpartDto.navn().fornavn()).isEqualTo("Ola");
        assertThat(annenpartDto.navn().etternavn()).isEqualTo("Nordmann");
        assertThat(annenpartDto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(32));
        assertThat(søkerinfo.arbeidsforhold()).isEmpty();
    }

    @Test
    void søker_med_flere_barn_hvor_det_ene_barnet_ikke_har_en_annenpart_registrert() {
        var søkerPdl = new Person();
        søkerPdl.setNavn(navn("Kari", null, "Kanari"));
        søkerPdl.setKjoenn(kjønn(KjoennType.KVINNE));
        søkerPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(28)));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setSivilstand(siviltilstand(Sivilstandstype.UGIFT));
        søkerPdl.setForelderBarnRelasjon(List.of(forelderBarnRelasjon(BARN_1_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR)));

        var annenpartTilBarn1Pdl = new Person();
        annenpartTilBarn1Pdl.setNavn(navn("Ola", null, "Nordmann"));
        annenpartTilBarn1Pdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(32)));
        annenpartTilBarn1Pdl.setDoedsfall(List.of());
        annenpartTilBarn1Pdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));

        var barn1Pdl = new Person();
        barn1Pdl.setNavn(navn("Barn", "Barnesen", "Den første"));
        barn1Pdl.setKjoenn(kjønn(KjoennType.KVINNE));
        barn1Pdl.setFoedselsdato(fødselsdato(LocalDate.now().minusMonths(11)));
        barn1Pdl.setDoedsfall(List.of());
        barn1Pdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));
        barn1Pdl.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN),
                forelderBarnRelasjon(ANNENPART_IDENT, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.BARN)
        ));
        var barn2Pdl = new Person();
        barn2Pdl.setNavn(navn("Barn", "Barnesen", "Den andre"));
        barn2Pdl.setKjoenn(kjønn(KjoennType.KVINNE));
        barn2Pdl.setFoedselsdato(fødselsdato(LocalDate.now().minusWeeks(2)));
        barn2Pdl.setDoedsfall(List.of());
        barn2Pdl.setAdressebeskyttelse(adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT));
        barn2Pdl.setForelderBarnRelasjon(List.of(
                forelderBarnRelasjon(SØKER_IDENT, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN)
                // Far har ikke fult ut farskasp erklæringen enda siden det bare er 2 uker siden fødsel
        ));

        when(pdlOppslagTjeneste.hentSøker(any())).thenReturn(new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søkerPdl));
        when(pdlOppslagTjeneste.hentBarnTilSøker(any())).thenReturn(List.of(
                new PdlOppslagTjeneste.PersonMedIdent(BARN_1_IDENT, barn1Pdl),
                new PdlOppslagTjeneste.PersonMedIdent(BARN_2_IDENT, barn2Pdl)
        ));
        when(pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(any(), any())).thenReturn(Map.of(BARN_1_IDENT, new PdlOppslagTjeneste.PersonMedIdent(ANNENPART_IDENT, annenpartTilBarn1Pdl)));
        when(krrSpråkKlient.finnSpråkkodeForBruker(any())).thenReturn(Målform.NB);
        when(kontaktInformasjonKlient.hentRegistertKontonummer()).thenReturn(new KontonummerDto("123456789", null));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        // Act
        var søkerinfo = oppslagTjeneste.personinfoMedArbeidsforholdFor();

        // Assert
        var søkerDto = søkerinfo.person();
        assertThat(søkerDto.navn().fornavn()).isEqualTo("Kari");
        assertThat(søkerDto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(søkerDto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(søkerDto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));
        assertThat(søkerDto.sivilstand()).isEqualTo(new no.nav.foreldrepenger.common.domain.felles.Sivilstand(no.nav.foreldrepenger.common.domain.felles.Sivilstand.Type.UGIFT));
        assertThat(søkerDto.målform()).isEqualTo(Målform.NB);
        assertThat(søkerDto.bankkonto().kontonummer()).isEqualTo("123456789");
        assertThat(søkerDto.barn()).hasSize(2);

        var barn1Dto = søkerDto.barn().get(0);
        assertThat(barn1Dto.navn().fornavn()).isEqualTo("Barn");
        assertThat(barn1Dto.navn().mellomnavn()).isEqualTo("Barnesen");
        assertThat(barn1Dto.navn().etternavn()).isEqualTo("Den Første");
        assertThat(barn1Dto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(barn1Dto.fødselsdato()).isEqualTo(LocalDate.now().minusMonths(11));
        assertThat(barn1Dto.annenPart().navn().fornavn()).isEqualTo("Ola");
        assertThat(barn1Dto.annenPart().navn().etternavn()).isEqualTo("Nordmann");
        assertThat(barn1Dto.annenPart().fødselsdato()).isEqualTo(LocalDate.now().minusYears(32));

        var barn2Dto = søkerDto.barn().get(1);
        assertThat(barn2Dto.navn().fornavn()).isEqualTo("Barn");
        assertThat(barn2Dto.navn().mellomnavn()).isEqualTo("Barnesen");
        assertThat(barn2Dto.navn().etternavn()).isEqualTo("Den Andre");
        assertThat(barn2Dto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(barn2Dto.fødselsdato()).isEqualTo(LocalDate.now().minusWeeks(2));
        assertThat(barn2Dto.annenPart()).isNull();

        assertThat(søkerinfo.arbeidsforhold()).isEmpty();
    }

    @Test
    void utenlandsk_kontonummer_mappes_riktig() {
        var søkerPdl = new Person();
        søkerPdl.setNavn(navn("Kari", null, "Kanari"));
        søkerPdl.setKjoenn(kjønn(KjoennType.KVINNE));
        søkerPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(28)));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setSivilstand(siviltilstand(Sivilstandstype.UGIFT));
        søkerPdl.setForelderBarnRelasjon(List.of());

        when(pdlOppslagTjeneste.hentSøker(any())).thenReturn(new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søkerPdl));
        when(pdlOppslagTjeneste.hentBarnTilSøker(any())).thenReturn(List.of());
        when(pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(any(), any())).thenReturn(Map.of());
        when(krrSpråkKlient.finnSpråkkodeForBruker(any())).thenReturn(Målform.NB);
        var sveitsBank = new KontonummerDto.UtenlandskKontoInfo("SVEITS BANK", null, null, null, null, null, null, null);
        var kontonummerDto = new KontonummerDto(null, sveitsBank);
        when(kontaktInformasjonKlient.hentRegistertKontonummer()).thenReturn(kontonummerDto);
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(List.of());

        // Act
        var søkerinfo = oppslagTjeneste.personinfoMedArbeidsforholdFor();

        // Assert
        assertThat(søkerinfo.person().bankkonto().kontonummer()).isNull();
        assertThat(søkerinfo.person().bankkonto().banknavn()).isEqualTo(sveitsBank.banknavn());
    }

    @Test
    void flere_arbeidsforhold_mappes_rikitg_fra_domenemodell_til_dto() {
        var søkerPdl = new Person();
        søkerPdl.setNavn(navn("Kari", null, "Kanari"));
        søkerPdl.setKjoenn(kjønn(KjoennType.KVINNE));
        søkerPdl.setFoedselsdato(fødselsdato(LocalDate.now().minusYears(28)));
        søkerPdl.setDoedfoedtBarn(List.of());
        søkerPdl.setSivilstand(siviltilstand(Sivilstandstype.UGIFT));
        søkerPdl.setForelderBarnRelasjon(List.of());

        var enkeltArbeidsforhold1 = new EksternArbeidsforholdDto(
                "2",
                "org",
                "Navn på arbeidsgiver 1", Stillingsprosent.arbeid(BigDecimal.valueOf(100)), LocalDate.now().minusYears(5),
                Optional.of(LocalDate.now())
        );
        var enkeltArbeidsforhold2 = new EksternArbeidsforholdDto(
                "12312312312",
                "org",
                "Navn på arbeidsgiver 2", Stillingsprosent.arbeid(BigDecimal.valueOf(100)), LocalDate.now().minusYears(5),
                Optional.empty()
        );
        var enkeltArbeidsforhold3 = new EksternArbeidsforholdDto(
                "1",
                "fnr",
                "Navn på arbeidsgiver 3", Stillingsprosent.arbeid(BigDecimal.valueOf(55)), LocalDate.now(),
                Optional.empty()
        );
        var eksterneArbeidsforhold = List.of(
                enkeltArbeidsforhold1,
                enkeltArbeidsforhold2,
                enkeltArbeidsforhold3
        );

        when(pdlOppslagTjeneste.hentSøker(any())).thenReturn(new PdlOppslagTjeneste.PersonMedIdent(SØKER_IDENT, søkerPdl));
        when(pdlOppslagTjeneste.hentBarnTilSøker(any())).thenReturn(List.of());
        when(pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(any(), any())).thenReturn(Map.of());
        when(krrSpråkKlient.finnSpråkkodeForBruker(any())).thenReturn(Målform.EN);
        when(kontaktInformasjonKlient.hentRegistertKontonummer()).thenReturn(new KontonummerDto("123456789", null));
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(eksterneArbeidsforhold);

        // Act
        var søkerinfo = oppslagTjeneste.personinfoMedArbeidsforholdFor();

        // Assert
        assertThat(søkerinfo.arbeidsforhold()).hasSameSizeAs(eksterneArbeidsforhold);

        var arbeidsforholdDto1 = søkerinfo.arbeidsforhold().get(0);
        assertThat(arbeidsforholdDto1.arbeidsgiverId()).isEqualTo("2");
        assertThat(arbeidsforholdDto1.arbeidsgiverNavn()).isEqualTo("Navn på arbeidsgiver 1");
        assertThat(arbeidsforholdDto1.stillingsprosent().prosent()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(arbeidsforholdDto1.from()).isEqualTo(LocalDate.now().minusYears(5));
        assertThat(arbeidsforholdDto1.to().get()).isEqualTo(LocalDate.now());

        var arbeidsforholdDto2 = søkerinfo.arbeidsforhold().get(1);
        assertThat(arbeidsforholdDto2.arbeidsgiverId()).isEqualTo("12312312312");
        assertThat(arbeidsforholdDto2.arbeidsgiverNavn()).isEqualTo("Navn på arbeidsgiver 2");
        assertThat(arbeidsforholdDto2.stillingsprosent().prosent()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(arbeidsforholdDto2.from()).isEqualTo(LocalDate.now().minusYears(5));
        assertThat(arbeidsforholdDto2.to()).isEmpty();

        var arbeidsforholdDto3 = søkerinfo.arbeidsforhold().get(2);
        assertThat(arbeidsforholdDto3.arbeidsgiverId()).isEqualTo("1");
        assertThat(arbeidsforholdDto3.arbeidsgiverNavn()).isEqualTo("Navn på arbeidsgiver 3");
        assertThat(arbeidsforholdDto3.stillingsprosent().prosent()).isEqualTo(BigDecimal.valueOf(55));
        assertThat(arbeidsforholdDto3.from()).isEqualTo(LocalDate.now());
        assertThat(arbeidsforholdDto2.to()).isEmpty();
    }
}