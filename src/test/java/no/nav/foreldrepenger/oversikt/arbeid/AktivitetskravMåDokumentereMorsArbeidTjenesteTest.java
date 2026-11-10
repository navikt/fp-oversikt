package no.nav.foreldrepenger.oversikt.arbeid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;

@ExtendWith(MockitoExtension.class)
class AktivitetskravMåDokumentereMorsArbeidTjenesteTest {
    private static final Fødselsnummer DUMMY_FNR_SØKER = new Fødselsnummer("123");
    private static final Fødselsnummer DUMMY_FNR_ANNENPART = new Fødselsnummer("345");
    private static final Fødselsnummer DUMMY_FNR_BARN = new Fødselsnummer("567");

    @Mock
    private PersonOppslagSystem personOppslagSystem;
    @Mock
    private AnnenPartSakTjeneste annenPartSakTjeneste;
    @Mock
    private KontaktInformasjonTjeneste kontaktInformasjonTjeneste;
    @Mock
    private AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste;


    private AktivitetskravMåDokumentereMorsArbeidTjeneste aktivitetskravMåDokumentereMorsArbeidTjeneste;

    @BeforeEach
    void setUp() {
        aktivitetskravMåDokumentereMorsArbeidTjeneste = new AktivitetskravMåDokumentereMorsArbeidTjeneste(
                personOppslagSystem,
                annenPartSakTjeneste,
                kontaktInformasjonTjeneste,
                aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste
        );
    }

    @Test
    void happy_case_hvor_mor_kan_varsles_og_har_arbeid_i_intervallet() {
        // Arrange
        when(kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(any())).thenReturn(false);
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(any(), any(), any(), any())).thenReturn(Optional.of(dummySak()));
        when(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste.krevesDokumentasjonForAktivitetskravArbeid(any())).thenReturn(false);

        // Act
        var krevesDokumentasjonForAktivitetskravArbeid = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());

        // Assert
        verify(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste, times(1)).krevesDokumentasjonForAktivitetskravArbeid(any());
        assertThat(krevesDokumentasjonForAktivitetskravArbeid).isFalse();
    }

    @Test
    void hvis_annenpart_har_reservert_seg_eller_kan_ikke_varsles_så_kreves_dokumentasjon_av_mors_arbeid() {
        // Arrange
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(any(), any(), any(), any())).thenReturn(Optional.of(dummySak()));
        when(kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(any())).thenReturn(true);
        // Act
        var krevesDokumentasjonForAktivitetskravArbeid = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());

        // Assert
        verify(kontaktInformasjonTjeneste, times(1)).harReservertSegEllerKanIkkeVarsles(any());
        assertThat(krevesDokumentasjonForAktivitetskravArbeid).isTrue();
    }

    @Test
    void hvis_annenpart_ikke_har_sak_men_barn_er_knyttet_til_søker_og_annenpart_skal_vi_sjekke_arbeidsforhold() {
        // Arrange
        when(kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(any())).thenReturn(false);
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(personOppslagSystem.barnHarDisseForeldrene(any(), any(), any())).thenReturn(true);
        when(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste.krevesDokumentasjonForAktivitetskravArbeid(any())).thenReturn(true);
        // Act
        var krevesDokumentasjonForAktivitetskravArbeid = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());

        // Assert
        verify(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste, times(1)).krevesDokumentasjonForAktivitetskravArbeid(any());
        assertThat(krevesDokumentasjonForAktivitetskravArbeid).isTrue();
    }

    @Test
    void hvis_annenpart_ikke_har_sak_og_barn_ikke_er_knyttet_til_begge_foreldrene_krever_vi_dokumentasjon() {
        // Arrange
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(personOppslagSystem.barnHarDisseForeldrene(any(), any(), any())).thenReturn(false);

        // Act
        var krevesDokumentasjonForAktivitetskravArbeid = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());

        // Assert
        verify(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste, never()).krevesDokumentasjonForAktivitetskravArbeid(any());
        assertThat(krevesDokumentasjonForAktivitetskravArbeid).isTrue();
    }

    @Test
    void ukjent_barn_i_pdl_gir_samme_svar_som_barn_uten_relasjon_til_foreldrene() throws BrukerIkkeFunnetIPdlException {
        // Arrange
        when(personOppslagSystem.aktørId(DUMMY_FNR_ANNENPART)).thenReturn(AktørId.dummy());
        when(personOppslagSystem.aktørId(DUMMY_FNR_BARN)).thenThrow(new BrukerIkkeFunnetIPdlException());
        when(personOppslagSystem.barnHarDisseForeldrene(any(), any(), any())).thenReturn(false);

        // Act
        var krevesDokumentasjonForAktivitetskravArbeid = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());

        // Assert: ukjent barn slås opp som ikke oppgitt barn
        verify(annenPartSakTjeneste).annenPartGjeldendeSakOppgittSøker(any(), any(), isNull(), any());
        verify(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste, never()).krevesDokumentasjonForAktivitetskravArbeid(any());
        assertThat(krevesDokumentasjonForAktivitetskravArbeid).isTrue();
    }

    @Test
    void ukjent_barn_gir_samme_svar_som_kjent_barn_når_annen_parts_sak_treffer_på_familiehendelse() throws BrukerIkkeFunnetIPdlException {
        // Arrange: annen parts sak matcher på familiehendelse uansett om barnet er kjent eller ikke
        when(personOppslagSystem.aktørId(DUMMY_FNR_ANNENPART)).thenReturn(AktørId.dummy());
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(any(), any(), any(), any())).thenReturn(Optional.of(dummySak()));
        when(kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(any())).thenReturn(false);
        when(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste.krevesDokumentasjonForAktivitetskravArbeid(any())).thenReturn(false);

        // Act
        when(personOppslagSystem.aktørId(DUMMY_FNR_BARN)).thenReturn(AktørId.dummy());
        var kjentBarn = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());
        when(personOppslagSystem.aktørId(DUMMY_FNR_BARN)).thenThrow(new BrukerIkkeFunnetIPdlException());
        var ukjentBarn = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());

        // Assert
        assertThat(ukjentBarn).isEqualTo(kjentBarn).isFalse();
        verify(annenPartSakTjeneste).annenPartGjeldendeSakOppgittSøker(any(), any(), isNull(), any());
    }

    @Test
    void ukjent_barn_gir_samme_svar_som_kjent_barn_uten_sak_og_relasjon() throws BrukerIkkeFunnetIPdlException {
        // Arrange: barnHarDisseForeldrene gir false både for kjent barn uten relasjon og for ukjent barn (se PdlKlientSystemTest)
        when(personOppslagSystem.aktørId(DUMMY_FNR_ANNENPART)).thenReturn(AktørId.dummy());
        when(personOppslagSystem.barnHarDisseForeldrene(any(), any(), any())).thenReturn(false);
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(any(), any(), any(), any())).thenReturn(Optional.empty());

        // Act
        when(personOppslagSystem.aktørId(DUMMY_FNR_BARN)).thenReturn(AktørId.dummy());
        var kjentBarnUtenSak = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());
        when(personOppslagSystem.aktørId(DUMMY_FNR_BARN)).thenThrow(new BrukerIkkeFunnetIPdlException());
        var ukjentBarn = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());

        // Assert
        assertThat(ukjentBarn).isEqualTo(kjentBarnUtenSak).isTrue();
        verify(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste, never()).krevesDokumentasjonForAktivitetskravArbeid(any());
    }

    @Test
    void ukjent_annen_part_i_pdl_gir_samme_svar_som_annen_part_uten_sak() throws BrukerIkkeFunnetIPdlException {
        // Arrange
        when(personOppslagSystem.aktørId(DUMMY_FNR_ANNENPART)).thenThrow(new BrukerIkkeFunnetIPdlException());
        when(personOppslagSystem.barnHarDisseForeldrene(any(), any(), any())).thenReturn(false);

        // Act
        var krevesDokumentasjonForAktivitetskravArbeid = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyRequestUttak());

        // Assert
        verify(annenPartSakTjeneste, never()).annenPartGjeldendeSakOppgittSøker(any(), any(), any(), any());
        verify(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste, never()).krevesDokumentasjonForAktivitetskravArbeid(any());
        assertThat(krevesDokumentasjonForAktivitetskravArbeid).isTrue();
    }

    @Test
    void happy_kombinert_case_hvor_mor_kan_varsles_og_har_arbeid_i_intervallet() {
        // Arrange
        when(kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(any())).thenReturn(false);
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(any(), any(), any(), any())).thenReturn(Optional.of(dummySak()));
        when(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste.krevesDokumentasjonForAktivitetskravArbeid(any())).thenReturn(false);

        // Act
        var krevesDokumentasjonForAktivitetskravArbeid = aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(DUMMY_FNR_SØKER, AktørId.dummy(), dummyKombinertRequestUttak());

        // Assert
        verify(aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste, times(1)).krevesDokumentasjonForAktivitetskravArbeid(any());
        assertThat(krevesDokumentasjonForAktivitetskravArbeid).isFalse();
    }

    private static ArbeidRest.MorArbeidRequest dummyRequestUttak() {
        return new ArbeidRest.MorArbeidRequest(DUMMY_FNR_ANNENPART, DUMMY_FNR_BARN, LocalDate.now(),
                List.of(new ArbeidRest.PeriodeRequest(LocalDate.now().minusWeeks(3), LocalDate.now(), null)));
    }

    private static ArbeidRest.MorArbeidRequest dummyKombinertRequestUttak() {
        return new ArbeidRest.MorArbeidRequest(DUMMY_FNR_ANNENPART, DUMMY_FNR_BARN, LocalDate.now(),
            List.of(new ArbeidRest.PeriodeRequest(LocalDate.now().minusWeeks(3), LocalDate.now(), PeriodeMedAktivitetskravType.UTTAK_BFHR),
                new ArbeidRest.PeriodeRequest(LocalDate.now().plusDays(1), LocalDate.now().plusWeeks(4), PeriodeMedAktivitetskravType.UTSETTELSE_BFHR),
                new ArbeidRest.PeriodeRequest(LocalDate.now().plusWeeks(4).plusDays(1), LocalDate.now().plusWeeks(6), PeriodeMedAktivitetskravType.UTTAK_BFHR)
            ));
    }

    private static SakFP0 dummySak() {
        return new SakFP0(Saksnummer.dummy(),
                AktørId.dummy(),
                false,
                Set.of(),
                AktørId.dummy(),
                null,
                Set.of(),
                Set.of(),
                BrukerRolle.MOR,
                Set.of(AktørId.dummy()),
                null,
                false,
                LocalDateTime.now());
    }
}
