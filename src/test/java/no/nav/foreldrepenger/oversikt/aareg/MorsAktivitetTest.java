package no.nav.foreldrepenger.oversikt.aareg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arbeid.AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.arbeid.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.arbeid.PeriodeMedAktivitetskravType;
import no.nav.foreldrepenger.oversikt.arbeid.PerioderMedAktivitetskravArbeid;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.AaregRestKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.ArbeidType;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.ArbeidsforholdRS;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.PermType;
import no.nav.fpsak.tidsserie.LocalDateSegment;

@ExtendWith(MockitoExtension.class)
class MorsAktivitetTest {

    private static final String ARBGIVER1 = "123456789";
    private static final String ARBGIVER2 = "123456780";
    private static final String ARBFORHOLD1 = "AltInnn-123";
    private static final String ARBFORHOLD2 = "AltInnn-456";
    private static final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);
    private static final BigDecimal FEMTI_PROSENT = new BigDecimal(50);
    private static final BigDecimal EN_PROSENT = BigDecimal.ONE;
    private static final BigDecimal NULL_PROSENT = BigDecimal.ZERO;
    private static final Fødselsnummer FNR = new Fødselsnummer("12345678901");

    @Mock
    private AaregRestKlient restKlient;

    private boolean kallTjeneste(PerioderMedAktivitetskravArbeid søknad, List<ArbeidsforholdRS> registerResponse) {
        when(restKlient.finnArbeidsforholdForArbeidstaker(any(), any(), any(), anyBoolean())).thenReturn(registerResponse);
        var tjeneste = new AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste(new ArbeidsforholdTjeneste(restKlient));
        return tjeneste.krevesDokumentasjonForAktivitetskravArbeid(søknad);
    }

    @Test
    void happy_case_ett_arbeidsforhold_som_matcher_søknad_trenger_ikke_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(uttakSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isFalse();
    }

    @Test
    void happy_case_ett_arbeidsforhold_femti_prosent_som_matcher_søknad_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(uttakSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isTrue();
    }

    @Test
    void happy_case_ett_arbeidsforhold_permisjon_som_matcher_søknad_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var perm = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(new ArbeidsforholdRS.PermisjonPermitteringRS(perm, FEMTI_PROSENT, PermType.ANNEN_PERMISJON_IKKE_LOVFESTET)),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(uttakSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isTrue();
    }

    @Test
    void happy_case_to_arbeidsforhold_som_matcher_søknad_trenger_ikke_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var response2 = new ArbeidsforholdRS(ARBFORHOLD2, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER2, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(uttakSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response1, response2));
        assertThat(resultat).isFalse();
    }

    @Test
    void to_arbeidsforhold_null_og_hundre_prosent_perm_hundre_prosent_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var perm = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(new ArbeidsforholdRS.PermisjonPermitteringRS(perm, HUNDRE_PROSENT, PermType.PERMISJON_MED_FORELDREPENGER)),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var response2 = new ArbeidsforholdRS(ARBFORHOLD2, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(NULL_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(uttakSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response1, response2));
        assertThat(resultat).isTrue();
    }

    @Test
    void to_arbeidsforhold_null_og_hundre_prosent_perm_null_prosent_trenger_ikke_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var perm = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var response2 = new ArbeidsforholdRS(ARBFORHOLD2, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(NULL_PROSENT, alltid)),
            List.of(new ArbeidsforholdRS.PermisjonPermitteringRS(perm, HUNDRE_PROSENT, PermType.PERMISJON_MED_FORELDREPENGER)),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(uttakSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response1, response2));
        assertThat(resultat).isFalse();
    }

    @Test
    void ett_arbeids_forhold_øker_stillingsgrad_under_søkt_periode_trenger_dokumentasjon() {
        var endringsdato = LocalDate.now().plusWeeks(1);
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var før = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, endringsdato.minusDays(1));
        var etter = new ArbeidsforholdRS.PeriodeRS(endringsdato, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, før), new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, etter)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(uttakSegment(LocalDate.now(), LocalDate.now().plusMonths(1))));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isTrue();
    }

    @Test
    void ett_arbeids_forhold_øker_stillingsgrad_før_søkt_periode_trenger_ikke_dokumentasjon() {
        var endringsdato = LocalDate.now().minusDays(1);
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var før = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, endringsdato.minusDays(1));
        var etter = new ArbeidsforholdRS.PeriodeRS(endringsdato, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(FEMTI_PROSENT, før), new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, etter)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(uttakSegment(LocalDate.now(), LocalDate.now().plusMonths(1))));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isFalse();
    }

    @Test
    void ett_arbeids_forhold_permisjon_mellom_søknadsperioder_trenger_ikke_dokumentasjon() {
        var permdato = LocalDate.now();
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var perm = new ArbeidsforholdRS.PeriodeRS(permdato.minusWeeks(2), permdato.plusWeeks(2).minusDays(1));
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(new ArbeidsforholdRS.PermisjonPermitteringRS(perm, HUNDRE_PROSENT, PermType.ANNEN_PERMISJON_IKKE_LOVFESTET)),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(
            uttakSegment(LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1).plusWeeks(1)),
            uttakSegment(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1).plusWeeks(1))
        ));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isFalse();
    }

    @Test
    void happy_case_ett_arbeidsforhold_som_matcher_søknad_utsettelse_trenger_ikke_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(EN_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(utsettelseSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isFalse();
    }

    @Test
    void happy_case_ett_arbeidsforhold_null_prosent_som_matcher_søknad_utsettelse_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(NULL_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(utsettelseSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isTrue();
    }

    @Test
    void happy_case_ett_arbeidsforhold_permisjon_som_matcher_søknad_utsettelse_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var perm = new ArbeidsforholdRS.PeriodeRS(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(EN_PROSENT, alltid)),
            List.of(new ArbeidsforholdRS.PermisjonPermitteringRS(perm, EN_PROSENT, PermType.ANNEN_PERMISJON_IKKE_LOVFESTET)),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(utsettelseSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isTrue();
    }

    @Test
    void happy_case_to_arbeidsforhold_som_matcher_søknad_utsettelse_trenger_ikke_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(EN_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var response2 = new ArbeidsforholdRS(ARBFORHOLD2, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER2, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(NULL_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(utsettelseSegment(LocalDate.now(), LocalDate.now().plusDays(1))));
        var resultat = kallTjeneste(søknad, List.of(response1, response2));
        assertThat(resultat).isFalse();
    }

    @Test
    void happy_kombinert_case_et_arbeidsforhold_som_matcher_søknad_uttak_utsettelse_trenger_ikke_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response1 = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(HUNDRE_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(
            uttakSegment(LocalDate.now(), LocalDate.now().plusDays(1)),
            utsettelseSegment(LocalDate.now().plusDays(2), LocalDate.now().plusDays(3))
        ));
        var resultat = kallTjeneste(søknad, List.of(response1));
        assertThat(resultat).isFalse();
    }

    @Test
    void happy_case_ett_arbeidsforhold_null_prosent_som_matcher_søknad_uttak_utsettelse_trenger_dokumentasjon() {
        var alltid = new ArbeidsforholdRS.PeriodeRS(LocalDate.MIN, LocalDate.MAX);
        var response = new ArbeidsforholdRS(ARBFORHOLD1, 123L,
            new ArbeidsforholdRS.OpplysningspliktigArbeidsgiverRS(ArbeidsforholdRS.OpplysningspliktigType.ORGANISASJON, ARBGIVER1, null, null),
            new ArbeidsforholdRS.AnsettelsesperiodeRS(alltid),
            List.of(new ArbeidsforholdRS.ArbeidsavtaleRS(NULL_PROSENT, alltid)),
            List.of(),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var søknad = new PerioderMedAktivitetskravArbeid(FNR, List.of(
            uttakSegment(LocalDate.now(), LocalDate.now().plusDays(1)),
            utsettelseSegment(LocalDate.now().plusDays(2), LocalDate.now().plusDays(3))
        ));
        var resultat = kallTjeneste(søknad, List.of(response));
        assertThat(resultat).isTrue();
    }

    private static LocalDateSegment<PeriodeMedAktivitetskravType> uttakSegment(LocalDate fom, LocalDate tom) {
        return new LocalDateSegment<>(fom, tom, PeriodeMedAktivitetskravType.UTTAK);
    }

    private static LocalDateSegment<PeriodeMedAktivitetskravType> utsettelseSegment(LocalDate fom, LocalDate tom) {
        return new LocalDateSegment<>(fom, tom, PeriodeMedAktivitetskravType.UTSETTELSE);
    }

}
