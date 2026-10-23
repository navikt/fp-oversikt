package no.nav.foreldrepenger.oversikt.tidslinje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.kontrakter.fpoversikt.inntektsmelding.FpOversiktInntektsmeldingDto;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeHistoriske;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpostSelvbetjening;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostType;
import no.nav.foreldrepenger.oversikt.arkiv.SafSelvbetjeningTjeneste;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.InntektsmeldingTjeneste;

class TidslinjeTjenesteTest {
        private static final Saksnummer DUMMY_SAKSNUMMER = new Saksnummer("0000000");
        private static final Fødselsnummer DUMMY_FNR = new Fødselsnummer("0000000");

        private TidslinjeTjeneste tjeneste;
        private SafSelvbetjeningTjeneste safselvbetjeningTjeneste;
        private InntektsmeldingTjeneste inntektsmeldingTjeneste;

        @BeforeEach
        void setUp() {
            safselvbetjeningTjeneste = mock(SafSelvbetjeningTjeneste.class);
            inntektsmeldingTjeneste = mock(InntektsmeldingTjeneste.class);
            tjeneste = new TidslinjeTjeneste(safselvbetjeningTjeneste, inntektsmeldingTjeneste);
        }

        @Test
        void søknadOgInntektmeldingTidslije() {
            var saksnummer = DUMMY_SAKSNUMMER;
            var søknadMedVedlegg = søknadMed1Vedlegg(saksnummer, LocalDateTime.now());
            var inntektsmelding = standardInntektsmelding(LocalDateTime.now());
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(List.of(søknadMedVedlegg));
            when(inntektsmeldingTjeneste.inntektsmeldinger(saksnummer)).thenReturn(List.of(inntektsmelding));

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje)
                    .hasSize(2)
                    .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
                    .containsExactly(
                            TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD,
                            TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING
                    );
        }

        @Test
        void søknadEtterlysIMOgVedtak() {
            var saksnummer = DUMMY_SAKSNUMMER;
            var søknadMedVedlegg = søknadMed1Vedlegg(saksnummer, LocalDateTime.now());
            var etterlysIM = etterlysIM(saksnummer);
            var vedtak = utgåendeVedtak(saksnummer, LocalDateTime.now());
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(
                    List.of(søknadMedVedlegg, etterlysIM, vedtak));

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje)
                    .hasSize(3)
                    .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
                    .containsExactly(
                            TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD,
                            TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_ETTERLYS_INNTEKTSMELDING,
                            TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK
                    );
        }

        @Test
        void søknadMedFritekstVedtak() {
            var saksnummer = DUMMY_SAKSNUMMER;
            var utgåendeVedtakFritekts = utgåendeVedtakFritekts(saksnummer, LocalDateTime.now());
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(
                    List.of(utgåendeVedtakFritekts));

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje)
                    .hasSize(1)
                    .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
                    .containsExactly(
                            TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK
                    );
        }

        @Test
        void søknadIMEttersendingVedtakEndringssøknadOgDeretterNyttVedtak() {
            var saksnummer = DUMMY_SAKSNUMMER;
            var tidspunkt = LocalDateTime.now();
            var søknadMedVedlegg = søknadMed1Vedlegg(saksnummer, tidspunkt);
            var inntektsmelding = standardInntektsmelding(tidspunkt.plusDays(1));
            var innhentOpplysningsBrev = innhentOpplysningsBrev(saksnummer, tidspunkt.plusDays(2));
            var ettersending = ettersender2Vedlegg(saksnummer, tidspunkt.plusDays(3));
            var vedtak = utgåendeVedtak(saksnummer, tidspunkt.plusDays(4));
            var endringssøknad = endringssøknadUtenVedlegg(saksnummer, tidspunkt.plusDays(4));
            var vedtakEndring = utgåendeVedtak(saksnummer, tidspunkt.plusDays(5));
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(
                    List.of(søknadMedVedlegg, innhentOpplysningsBrev, ettersending, vedtak, endringssøknad, vedtakEndring));
            when(inntektsmeldingTjeneste.inntektsmeldinger(saksnummer)).thenReturn(List.of(inntektsmelding));

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje)
                    .hasSize(7)
                    .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
                    .containsExactly(
                            TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD,
                            TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING,
                            TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_INNHENT_OPPLYSNINGER,
                            TidslinjeHendelseDto.TidslinjeHendelseType.ETTERSENDING,
                            TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK,
                            TidslinjeHendelseDto.TidslinjeHendelseType.ENDRINGSSØKNAD,
                            TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK
                    );
        }

        @Test
        void nyeSøknaderFørVedtakSkalFåTypeFørstegangssøknadNY() {
            var saksnummer = DUMMY_SAKSNUMMER;
            var tidspunkt = LocalDateTime.now();
            var søknadMedVedlegg = søknadMed1Vedlegg(saksnummer, tidspunkt);
            var inntektsmelding = standardInntektsmelding(tidspunkt.plusDays(1));
            var ettersending = ettersender2Vedlegg(saksnummer, tidspunkt.plusDays(2));
            var nySøknadMedVedlegg = søknadMed1Vedlegg(saksnummer, tidspunkt.plusDays(3));
            var vedtak = utgåendeVedtak(saksnummer, tidspunkt.plusDays(4));
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(List.of(søknadMedVedlegg, ettersending, nySøknadMedVedlegg, vedtak));
            when(inntektsmeldingTjeneste.inntektsmeldinger(saksnummer)).thenReturn(List.of(inntektsmelding));

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje)
                    .hasSize(5)
                    .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
                    .containsExactly(
                            TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD,
                            TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING,
                            TidslinjeHendelseDto.TidslinjeHendelseType.ETTERSENDING,
                            TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD_NY,
                            TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK
                    );
        }

        @Test
        void korrigertUttalelseTilbakebatlingMappersRiktig() {
            var saksnummer = DUMMY_SAKSNUMMER;
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(List.of(korrigertVarselTilbakebetaling(saksnummer, LocalDateTime.now())));
            when(inntektsmeldingTjeneste.inntektsmeldinger(saksnummer)).thenReturn(List.of());

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje)
                    .hasSize(1)
                    .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
                    .containsExactly(
                            TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_VARSEL_TILBAKEBETALING
                    );
        }

        @Test
        void annetUtgåendeBrevFraFpTilbakeSomIkkeErVarselSkalIkkeMappes() {
            var saksnummer = DUMMY_SAKSNUMMER;
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(List.of(innhentOpplysningTilbakebetaling(saksnummer, LocalDateTime.now())));
            when(inntektsmeldingTjeneste.inntektsmeldinger(saksnummer)).thenReturn(List.of());

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje).isEmpty();
        }

        @Test
        void skalFiltrereBortInnteksmeldingFraJoark() {
            var saksnummer = DUMMY_SAKSNUMMER;
            var søknadMedVedlegg = søknadMed1Vedlegg(saksnummer, LocalDateTime.now());
            var innteksmeldingJournalpost = innteksmeldingJournalpost(saksnummer);
            var inntektsmelding = standardInntektsmelding(LocalDateTime.now());
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(
                    List.of(søknadMedVedlegg, innteksmeldingJournalpost));
            when(inntektsmeldingTjeneste.inntektsmeldinger(saksnummer)).thenReturn(List.of(inntektsmelding));

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje)
                    .hasSize(2)
                    .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
                    .containsExactly(
                            TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD,
                            TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING
                    );
        }

        @Test
        void tidslinjenSorteresEtterOpprettetTidspunkt() {
            var saksnummer = DUMMY_SAKSNUMMER;
            var tidspunkt = LocalDateTime.now();
            var inntektsmelding = standardInntektsmelding(tidspunkt);
            var søknadMedVedlegg = søknadMed1Vedlegg(saksnummer, tidspunkt.plusSeconds(1));
            var vedtak = utgåendeVedtak(saksnummer, tidspunkt.plusSeconds(2));
            when(safselvbetjeningTjeneste.alleJournalposter(DUMMY_FNR, saksnummer)).thenReturn(List.of(vedtak, søknadMedVedlegg)); // Reversert med vilje
            when(inntektsmeldingTjeneste.inntektsmeldinger(saksnummer)).thenReturn(List.of(inntektsmelding));

            var tidslinje = tjeneste.tidslinje(DUMMY_FNR, saksnummer);

            assertThat(tidslinje)
                    .hasSize(3)
                    .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
                    .containsExactly(
                            TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING,
                            TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD,
                            TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK);
        }

        public static EnkelJournalpostSelvbetjening søknadMed1Vedlegg(Saksnummer saksnummer, LocalDateTime mottatt) {
            return new EnkelJournalpostSelvbetjening(
                    DokumentTypeHistoriske.I000003.getTittel(),
                    "1",
                    saksnummer.value(),
                    JournalpostType.INNGÅENDE_DOKUMENT, mottatt,
                    DokumentTypeHistoriske.I000003,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", DokumentTypeHistoriske.I000003.getTittel(), null),
                            new EnkelJournalpostSelvbetjening.Dokument("2", DokumentTypeHistoriske.I000036.getTittel(), null)
                    )
            );
        }

        public static EnkelJournalpostSelvbetjening endringssøknadUtenVedlegg(Saksnummer saksnummer, LocalDateTime tidspunkt) {
            return new EnkelJournalpostSelvbetjening(
                    DokumentTypeHistoriske.I000050.getTittel(),
                    "2",
                    saksnummer.value(),
                    JournalpostType.INNGÅENDE_DOKUMENT,
                    tidspunkt,
                    DokumentTypeHistoriske.I000050,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", DokumentTypeHistoriske.I000050.getTittel(), null)
                    )
            );
        }

        public static EnkelJournalpostSelvbetjening ettersender2Vedlegg(Saksnummer saksnummer, LocalDateTime tidspunkt) {
            return new EnkelJournalpostSelvbetjening(
                    DokumentTypeHistoriske.I000036.getTittel(),
                    "3",
                    saksnummer.value(),
                    JournalpostType.INNGÅENDE_DOKUMENT, tidspunkt,
                    DokumentTypeHistoriske.I000023,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", DokumentTypeHistoriske.I000023.getTittel(), null),
                            new EnkelJournalpostSelvbetjening.Dokument("2", DokumentTypeHistoriske.I000036.getTittel(), null)
                    )
            );
        }

        public static EnkelJournalpostSelvbetjening innteksmeldingJournalpost(Saksnummer saksnummer) {
            return new EnkelJournalpostSelvbetjening(
                    DokumentTypeHistoriske.I000067.getTittel(),
                    "4",
                    saksnummer.value(),
                    JournalpostType.INNGÅENDE_DOKUMENT,
                    LocalDateTime.now(),
                    DokumentTypeHistoriske.I000067,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", DokumentTypeHistoriske.I000067.getTittel(), null)
                    )
            );
        }

        public static EnkelJournalpostSelvbetjening utgåendeVedtakFritekts(Saksnummer saksnummer, LocalDateTime mottatt) {
            return new EnkelJournalpostSelvbetjening(
                    "Innvilgelsesbrev foreldrepenger",
                    "5",
                    saksnummer.value(),
                    JournalpostType.UTGÅENDE_DOKUMENT, mottatt,
                    null,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", null, EnkelJournalpostSelvbetjening.Brevkode.FRITEKSTBREV)
                    )
            );
        }


        public static EnkelJournalpostSelvbetjening utgåendeVedtak(Saksnummer saksnummer, LocalDateTime mottatt) {
            return new EnkelJournalpostSelvbetjening(
                    "Innvilgelsesbrev foreldrepenger",
                    "5",
                    saksnummer.value(),
                    JournalpostType.UTGÅENDE_DOKUMENT, mottatt,
                    null,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", null, EnkelJournalpostSelvbetjening.Brevkode.FORELDREPENGER_INNVILGELSE)
                    )
            );
        }

        public static EnkelJournalpostSelvbetjening innhentOpplysningTilbakebetaling(Saksnummer saksnummer, LocalDateTime mottatt) {
            return new EnkelJournalpostSelvbetjening(
                    "Innhent opp",
                    "15",
                    saksnummer.value(),
                    JournalpostType.UTGÅENDE_DOKUMENT, mottatt,
                    null,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", null, EnkelJournalpostSelvbetjening.Brevkode.VARSEL_TILBAKEBETALING_FP)
                    )
            );
        }

        public static EnkelJournalpostSelvbetjening korrigertVarselTilbakebetaling(Saksnummer saksnummer, LocalDateTime mottatt) {
            return new EnkelJournalpostSelvbetjening(
                    "Korrigert Varsel tilbakebetaling",
                    "15",
                    saksnummer.value(),
                    JournalpostType.UTGÅENDE_DOKUMENT, mottatt,
                    null,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", null, EnkelJournalpostSelvbetjening.Brevkode.VARSEL_TILBAKEBETALING_FP)
                    )
            );
        }


        public static EnkelJournalpostSelvbetjening innhentOpplysningsBrev(Saksnummer saksnummer, LocalDateTime tidspunkt) {
            return new EnkelJournalpostSelvbetjening(
                    "Innhente opplysninger",
                    "5",
                    saksnummer.value(),
                    JournalpostType.UTGÅENDE_DOKUMENT, tidspunkt,
                    null,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", null, EnkelJournalpostSelvbetjening.Brevkode.INNHENTE_OPPLYSNINGER)
                    )
            );
        }


        public static EnkelJournalpostSelvbetjening etterlysIM(Saksnummer saksnummer) {
            return new EnkelJournalpostSelvbetjening(
                    "Etterlys inntektsmelding",
                    "6",
                    saksnummer.value(),
                    JournalpostType.UTGÅENDE_DOKUMENT,
                    LocalDateTime.now(),
                    null,
                    List.of(
                            new EnkelJournalpostSelvbetjening.Dokument("1", null, EnkelJournalpostSelvbetjening.Brevkode.ETTERLYS_INNTEKTSMELDING)
                    )
            );
        }

        public static FpOversiktInntektsmeldingDto standardInntektsmelding(LocalDateTime opprettet) {
            return new FpOversiktInntektsmeldingDto(
                    1, true, null, null, null, null, null,  null, opprettet, null, Collections.emptyList(), Collections.emptyList()
            );
        }
    }
