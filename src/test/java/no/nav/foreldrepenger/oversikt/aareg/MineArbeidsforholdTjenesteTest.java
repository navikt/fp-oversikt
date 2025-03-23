package no.nav.foreldrepenger.oversikt.aareg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.ereg.VirksomhetTjeneste;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;

@ExtendWith(MockitoExtension.class)
class MineArbeidsforholdTjenesteTest {

    private static final String ARBGIVER1 = "123456789";
    private static final String ARBGIVER2A = "1234567891111";
    private static final String ARBGIVER2F = "12345678922";
    private static final String ARBFORHOLD1 = "AltInnn-123";
    private static final String ARBFORHOLD2 = "AltInnn-456";
    private static final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);
    private static final BigDecimal FEMTI_PROSENT = new BigDecimal(50);
    private static final BigDecimal NULL_PROSENT = BigDecimal.ZERO;
    private static final AktørId AKTOER_ID = new AktørId("1234567890123");
    private static final Fødselsnummer FNR = new Fødselsnummer("12345678901");


    @Mock
    private AaregRestKlient restKlient;
    @Mock
    private PersonOppslagSystem personOppslagSystem;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;

    @BeforeEach
    void setUp() {
        when(personOppslagSystem.fødselsnummer(AKTOER_ID)).thenReturn(FNR);
    }

    private List<EksternArbeidsforhold> kallTjeneste(List<ArbeidsforholdRS> registerResponse) {
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), anyBoolean())).thenReturn(registerResponse);
        lenient().when(virksomhetTjeneste.hentOrganisasjonNavn(any())).thenReturn("Virksomhet");
        lenient().when(personOppslagSystem.navn(any())).thenReturn("Fornavn Etternavn");
        var tjeneste = new MineArbeidsforholdTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem, virksomhetTjeneste);
        return tjeneste.brukersArbeidsforhold(AKTOER_ID);
    }

    @Test
    void happy_case_ett_arbeidsforhold_ordinært_arbeid() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var resultat = kallTjeneste(List.of(response)).getFirst();
        assertThat(resultat.arbeidsgiverId()).isEqualTo(ARBGIVER1);
        assertThat(resultat.arbeidsgiverIdType()).isEqualTo("orgnr");
        assertThat(resultat.from()).isEqualTo(LocalDate.MIN);
        assertThat(resultat.to()).isEmpty();
        assertThat(resultat.stillingsprosent().prosent()).isEqualByComparingTo(HUNDRE_PROSENT);
        assertThat(resultat.arbeidsgiverNavn()).isEqualTo("Virksomhet");
    }

    @Test
    void happy_case_ett_arbeidsforhold_forenklet_oppgjørsordning() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusMonths(1), LocalDate.now().plusWeeks(2));
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.PERSON, null, ARBGIVER2A, ARBGIVER2F),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(),
            List.of(),
            ArbeidType.FORENKLET_OPPGJØRSORDNING);

        var resultat = kallTjeneste(List.of(response)).getFirst();
        assertThat(resultat.arbeidsgiverId()).isEqualTo(ARBGIVER2F);
        assertThat(resultat.arbeidsgiverIdType()).isEqualTo("fnr");
        assertThat(resultat.from()).isEqualTo(LocalDate.now().minusMonths(1));
        assertThat(resultat.to()).hasValueSatisfying(d -> assertThat(d).isEqualTo(LocalDate.now().plusWeeks(2)));
        assertThat(resultat.stillingsprosent()).isNull();
        assertThat(resultat.arbeidsgiverNavn()).isEqualTo("Fornavn Etternavn");
    }

    @Test
    void happy_case_ett_arbeidsforhold_ordinært_arbeid_økende_prosent() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.now().plusYears(1));
        var endring = LocalDate.now().minusWeeks(1);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, endring.minusDays(1))),
                new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, new ArbeidsforholdRS.PeriodeRS(endring, LocalDate.MAX))),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var resultat = kallTjeneste(List.of(response)).getFirst();
        assertThat(resultat.arbeidsgiverId()).isEqualTo(ARBGIVER1);
        assertThat(resultat.from()).isEqualTo(LocalDate.MIN);
        assertThat(resultat.to()).hasValueSatisfying(d -> assertThat(d).isEqualTo(LocalDate.now().plusYears(1)));
        assertThat(resultat.stillingsprosent().prosent()).isEqualByComparingTo(HUNDRE_PROSENT);
        assertThat(resultat.arbeidsgiverNavn()).isEqualTo("Virksomhet");
    }

    @Test
    void happy_case_to_arbeidsforhold_ordinært_arbeid() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var response2 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(NULL_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var resultat = kallTjeneste(List.of(response1, response2));
        assertThat(resultat).hasSize(2)
            .satisfiesOnlyOnce(d -> assertThat(d.stillingsprosent().prosent()).isEqualTo(new BigDecimal(100)))
            .satisfiesOnlyOnce(d -> assertThat(d.stillingsprosent().prosent()).isEqualTo(BigDecimal.ZERO));
    }

    @Test
    void frilans_to_oppdrag() {
        var periode1 = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusMonths(3).minusWeeks(1), LocalDate.now().minusMonths(3));
        var periode2 = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(1));
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(periode1),
            List.of(),
            List.of(),
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);
        var response2 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(periode2),
            List.of(),
            List.of(),
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);

        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), any(), anyBoolean())).thenReturn(List.of(response1, response2));
        when(virksomhetTjeneste.hentOrganisasjonNavn(any())).thenReturn("Virksomhet");
        var tjeneste = new MineArbeidsforholdTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem, virksomhetTjeneste);

        var resultat = tjeneste.brukersFrilansoppdragSisteSeksMåneder(AKTOER_ID);
        assertThat(resultat).hasSize(2)
            .satisfies(d -> assertThat(d.stream().allMatch(a -> a.stillingsprosent() == null)).isTrue());
    }

}
