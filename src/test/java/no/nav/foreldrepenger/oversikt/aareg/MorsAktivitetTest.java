package no.nav.foreldrepenger.oversikt.aareg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ExtendWith(MockitoExtension.class)
class MorsAktivitetTest {

    private static final String ARBGIVER1 = "123456789";
    private static final String ARBGIVER2 = "123456780";
    private static final String ARBFORHOLD1 = "AltInnn-123";
    private static final String ARBFORHOLD2 = "AltInnn-456";
    private static final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);
    private static final BigDecimal FEMTI_PROSENT = new BigDecimal(50);
    private static final BigDecimal NULL_PROSENT = BigDecimal.ZERO;
    private static final AktørId AKTOER_ID = AktørId.dummy();
    private static final Fødselsnummer FNR = new Fødselsnummer(AKTOER_ID.value().substring(0, 10));

    @Mock
    private AaregRestKlient restKlient;
    @Mock
    private PersonOppslagSystem personOppslagSystem;

    @BeforeEach
    void setUp() {
        when(personOppslagSystem.fødselsnummer(AKTOER_ID)).thenReturn(FNR);
    }

    @Test
    void happy_case_ett_arbeidsforhold_som_matcher_søknad_trenger_ikke_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), eq(true))).thenReturn(List.of(response));
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem);

        var søknad = new PerioderMedAktivitetskravArbeid(AKTOER_ID, List.of(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1))));
        assertThat(tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad)).isFalse();
    }

    @Test
    void happy_case_ett_arbeidsforhold_femti_prosent_som_matcher_søknad_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), eq(true))).thenReturn(List.of(response));
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem);

        var søknad = new PerioderMedAktivitetskravArbeid(AKTOER_ID, List.of(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1))));
        assertThat(tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad)).isTrue();
    }

    @Test
    void happy_case_ett_arbeidsforhold_permisjon_som_matcher_søknad_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var perm = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(new ArbeidsforholdRS.PermisjonPermitteringRS(perm, FEMTI_PROSENT, PermType.ANNEN_PERMISJON_IKKE_LOVFESTET)),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), eq(true))).thenReturn(List.of(response));
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem);

        var søknad = new PerioderMedAktivitetskravArbeid(AKTOER_ID, List.of(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1))));
        assertThat(tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad)).isTrue();
    }

    @Test
    void happy_case_to_arbeidsforhold_som_matcher_søknad_trenger_ikke_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var response2 = new ArbeidsforholdRS(ARBFORHOLD2, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER2, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), eq(true))).thenReturn(List.of(response1, response2));
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem);

        var søknad = new PerioderMedAktivitetskravArbeid(AKTOER_ID, List.of(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1))));
        assertThat(tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad)).isFalse();
    }

    @Test
    void diskusjonstilfelle_to_arbeidsforhold_som_matcher_søknad_permisjon_fra_null_prosent_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var perm = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var response2 = new ArbeidsforholdRS(ARBFORHOLD2, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(NULL_PROSENT, alltid)),
            List.of(new ArbeidsforholdRS.PermisjonPermitteringRS(perm, HUNDRE_PROSENT, PermType.PERMISJON_MED_FORELDREPENGER)),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), eq(true))).thenReturn(List.of(response1, response2));
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem);

        var søknad = new PerioderMedAktivitetskravArbeid(AKTOER_ID, List.of(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1))));
        assertThat(tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad)).isTrue();
    }

    @Test
    void ett_arbeids_forhold_øker_stillingsgrad_under_søkt_periode_trenger_dokumentasjon() {
        var endringsdato = LocalDate.now().plusWeeks(1);
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var før = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, endringsdato.minusDays(1));
        var etter = new ArbeidsforholdRS.PeriodeRS(endringsdato, LocalDate.MAX);
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, før), new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, etter)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), eq(true))).thenReturn(List.of(response1));
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem);

        var søknad = new PerioderMedAktivitetskravArbeid(AKTOER_ID, List.of(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusMonths(1))));
        assertThat(tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad)).isTrue();
    }

    @Test
    void ett_arbeids_forhold_øker_stillingsgrad_før_søkt_periode_trenger_ikke_dokumentasjon() {
        var endringsdato = LocalDate.now().minusDays(1);
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var før = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, endringsdato.minusDays(1));
        var etter = new ArbeidsforholdRS.PeriodeRS(endringsdato, LocalDate.MAX);
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, før), new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, etter)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), eq(true))).thenReturn(List.of(response1));
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem);

        var søknad = new PerioderMedAktivitetskravArbeid(AKTOER_ID, List.of(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusMonths(1))));
        assertThat(tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad)).isFalse();
    }

    @Test
    void ett_arbeids_forhold_perminsjon_mellom_søknadsperioder_trenger_ikke_dokumentasjon() {
        var permdato = LocalDate.now();
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var perm = new ArbeidsforholdRS.PeriodeRS(permdato.minusWeeks(2), permdato.plusWeeks(2).minusDays(1));
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.Organisasjon, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(new ArbeidsforholdRS.PermisjonPermitteringRS(perm, HUNDRE_PROSENT, PermType.ANNEN_PERMISJON_IKKE_LOVFESTET)),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), eq(true))).thenReturn(List.of(response1));
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravTjeneste(new ArbeidsforholdTjeneste(restKlient), personOppslagSystem);

        var søknad = new PerioderMedAktivitetskravArbeid(AKTOER_ID, List.of(
            new LocalDateInterval(LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1).plusWeeks(1)),
            new LocalDateInterval(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1).plusWeeks(1))
        ));
        assertThat(tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad)).isFalse();
    }

}
