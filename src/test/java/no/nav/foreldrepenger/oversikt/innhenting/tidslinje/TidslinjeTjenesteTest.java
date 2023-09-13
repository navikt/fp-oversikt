package no.nav.foreldrepenger.oversikt.innhenting.tidslinje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.arkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Beløp;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingV1;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;
import no.nav.foreldrepenger.oversikt.stub.InntektsmeldingRepositoryStub;

class TidslinjeTjenesteTest {

    private static final String ORGNUMMER_DUMMY = "99999999";
    private static final Saksnummer SAKSNUMMER_DUMMY = Saksnummer.dummy();

    private TidslinjeTjeneste tjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private InntektsmeldingerRepository inntektsmeldingerRepository;

    @BeforeEach
    void setUp() {
        dokumentArkivTjeneste = mock(DokumentArkivTjeneste.class);
        inntektsmeldingerRepository = new InntektsmeldingRepositoryStub();
        tjeneste = new TidslinjeTjeneste(dokumentArkivTjeneste, inntektsmeldingerRepository);
    }

    @Test
    void søknadOgInntektmeldingTidslije() {
        var søknadMedVedlegg = søknadMedVedlegg();
        var inntektsmelding = standardInntektsmelding();
        when(dokumentArkivTjeneste.hentAlleJournalposter(SAKSNUMMER_DUMMY)).thenReturn(List.of(søknadMedVedlegg));
        inntektsmeldingerRepository.lagre(SAKSNUMMER_DUMMY, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(SAKSNUMMER_DUMMY);

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
        var søknadMedVedlegg = søknadMedVedlegg();
        var etterlysIM = etterlysIM();
        var vedtak = utgåendeVedtak();
        when(dokumentArkivTjeneste.hentAlleJournalposter(SAKSNUMMER_DUMMY)).thenReturn(
            List.of(søknadMedVedlegg, etterlysIM, vedtak));

        var tidslinje = tjeneste.tidslinje(SAKSNUMMER_DUMMY);

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
    void søknadIMEttersendingVedtakEndringssøknadOgDeretterNyttVedtak() {
        var søknadMedVedlegg = søknadMedVedlegg();
        var inntektsmelding = standardInntektsmelding();
        var innhentOpplysningsBrev = innhentOpplysningsBrev();
        var ettersending = ettersenderVedlegg();
        var vedtak = utgåendeVedtak();
        var endringssøknad = endringssøknadUtenVedlegg();
        var vedtakEndring = utgåendeVedtak();
        when(dokumentArkivTjeneste.hentAlleJournalposter(SAKSNUMMER_DUMMY)).thenReturn(
            List.of(søknadMedVedlegg, innhentOpplysningsBrev, ettersending, vedtak, endringssøknad, vedtakEndring));
        inntektsmeldingerRepository.lagre(SAKSNUMMER_DUMMY, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(SAKSNUMMER_DUMMY);

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
    void skalFiltrereBortInnteksmeldingFraJoark() {
        var søknadMedVedlegg = søknadMedVedlegg();
        var innteksmeldingJournalpost = innteksmeldingJournalpost();
        var inntektsmelding = standardInntektsmelding();
        when(dokumentArkivTjeneste.hentAlleJournalposter(SAKSNUMMER_DUMMY)).thenReturn(
            List.of(søknadMedVedlegg, innteksmeldingJournalpost));
        inntektsmeldingerRepository.lagre(SAKSNUMMER_DUMMY, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(SAKSNUMMER_DUMMY);

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
        var inntektsmelding = standardInntektsmelding();
        var søknadMedVedlegg = søknadMedVedlegg();
        var vedtak = utgåendeVedtak();
        when(dokumentArkivTjeneste.hentAlleJournalposter(SAKSNUMMER_DUMMY)).thenReturn(List.of(vedtak, søknadMedVedlegg)); // Reversert med vilje
        inntektsmeldingerRepository.lagre(SAKSNUMMER_DUMMY, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(SAKSNUMMER_DUMMY);

        assertThat(tidslinje)
            .hasSize(3)
            .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
            .containsExactly(
                TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING,
                TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD,
                TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK);
    }

    private static EnkelJournalpost søknadMedVedlegg() {
        return new EnkelJournalpost(
            DokumentTypeId.I000003.getTittel(),
            "1",
            SAKSNUMMER_DUMMY.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            DokumentTypeId.I000003,
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", DokumentTypeId.I000003, null),
                new EnkelJournalpost.Dokument("2", DokumentTypeId.I000036, null)
            )
        );
    }

    private static EnkelJournalpost endringssøknadUtenVedlegg() {
        return new EnkelJournalpost(
            DokumentTypeId.I000050.getTittel(),
            "2",
            SAKSNUMMER_DUMMY.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            DokumentTypeId.I000050,
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", DokumentTypeId.I000050, null)
            )
        );
    }

    private static EnkelJournalpost ettersenderVedlegg() {
        return new EnkelJournalpost(
            DokumentTypeId.I000036.getTittel(),
            "3",
            SAKSNUMMER_DUMMY.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            DokumentTypeId.I000023,
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", DokumentTypeId.I000023, null),
                new EnkelJournalpost.Dokument("2", DokumentTypeId.I000036, null)
            )
        );
    }

    private static EnkelJournalpost innteksmeldingJournalpost() {
        return new EnkelJournalpost(
            DokumentTypeId.I000067.getTittel(),
            "4",
            SAKSNUMMER_DUMMY.value(),
            new EnkelJournalpost.Bruker(ORGNUMMER_DUMMY, EnkelJournalpost.Bruker.Type.ORGNR),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            DokumentTypeId.I000067,
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", DokumentTypeId.I000067, null)
            )
        );
    }


    private static EnkelJournalpost utgåendeVedtak() {
        return new EnkelJournalpost(
            "Innvilgelsesbrev foreldrepenger",
            "5",
            SAKSNUMMER_DUMMY.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            null, // Todo: Dokumentet har vel ikke en hovedtype her? Elller?
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", null, EnkelJournalpost.Brevkode.INVFOR)
            )
        );
    }


    private static EnkelJournalpost innhentOpplysningsBrev() {
        return new EnkelJournalpost(
            "Innhente opplysninger",
            "5",
            SAKSNUMMER_DUMMY.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            null, // Todo: Dokumentet har vel ikke en hovedtype her? Elller?
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", null, EnkelJournalpost.Brevkode.INNOPP)
            )
        );
    }


    private static EnkelJournalpost etterlysIM() {
        return new EnkelJournalpost(
            "Etterlys inntektsmelding",
            "6",
            SAKSNUMMER_DUMMY.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            null, // Todo: Dokumentet har vel ikke en hovedtype her? Elller?
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", null, EnkelJournalpost.Brevkode.ELYSIM)
            )
        );
    }

    private static Inntektsmelding standardInntektsmelding() {
        return new InntektsmeldingV1("7", new Arbeidsgiver(ORGNUMMER_DUMMY), LocalDateTime.now(), Beløp.ZERO);
    }
}
