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
        var saksnummer = Saksnummer.dummy();
        var søknadMedVedlegg = søknadMedVedlegg(saksnummer, LocalDateTime.now());
        var inntektsmelding = standardInntektsmelding(LocalDateTime.now());
        when(dokumentArkivTjeneste.hentAlleJournalposter(saksnummer)).thenReturn(List.of(søknadMedVedlegg));
        inntektsmeldingerRepository.lagre(saksnummer, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(saksnummer);

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
        var saksnummer = Saksnummer.dummy();
        var søknadMedVedlegg = søknadMedVedlegg(saksnummer, LocalDateTime.now());
        var etterlysIM = etterlysIM(saksnummer);
        var vedtak = utgåendeVedtak(saksnummer, LocalDateTime.now());
        when(dokumentArkivTjeneste.hentAlleJournalposter(saksnummer)).thenReturn(
            List.of(søknadMedVedlegg, etterlysIM, vedtak));

        var tidslinje = tjeneste.tidslinje(saksnummer);

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
        var saksnummer = Saksnummer.dummy();
        var tidspunkt = LocalDateTime.now();
        var søknadMedVedlegg = søknadMedVedlegg(saksnummer, tidspunkt);
        var inntektsmelding = standardInntektsmelding(tidspunkt.plusDays(1));
        var innhentOpplysningsBrev = innhentOpplysningsBrev(saksnummer, tidspunkt.plusDays(2));
        var ettersending = ettersenderVedlegg(saksnummer, tidspunkt.plusDays(3));
        var vedtak = utgåendeVedtak(saksnummer, tidspunkt.plusDays(4));
        var endringssøknad = endringssøknadUtenVedlegg(saksnummer, tidspunkt.plusDays(4));
        var vedtakEndring = utgåendeVedtak(saksnummer, tidspunkt.plusDays(5));
        when(dokumentArkivTjeneste.hentAlleJournalposter(saksnummer)).thenReturn(
            List.of(søknadMedVedlegg, innhentOpplysningsBrev, ettersending, vedtak, endringssøknad, vedtakEndring));
        inntektsmeldingerRepository.lagre(saksnummer, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(saksnummer);

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
        var saksnummer = Saksnummer.dummy();
        var tidspunkt = LocalDateTime.now();
        var søknadMedVedlegg = søknadMedVedlegg(saksnummer, tidspunkt);
        var inntektsmelding = standardInntektsmelding(tidspunkt.plusDays(1));
        var ettersending = ettersenderVedlegg(saksnummer, tidspunkt.plusDays(2));
        var nySøknadMedVedlegg = søknadMedVedlegg(saksnummer, tidspunkt.plusDays(3));
        var vedtak = utgåendeVedtak(saksnummer, tidspunkt.plusDays(4));
        when(dokumentArkivTjeneste.hentAlleJournalposter(saksnummer)).thenReturn(List.of(søknadMedVedlegg, ettersending, nySøknadMedVedlegg, vedtak));
        inntektsmeldingerRepository.lagre(saksnummer, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(saksnummer);

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
    void skalFiltrereBortInnteksmeldingFraJoark() {
        var saksnummer = Saksnummer.dummy();
        var søknadMedVedlegg = søknadMedVedlegg(saksnummer, LocalDateTime.now());
        var innteksmeldingJournalpost = innteksmeldingJournalpost(saksnummer);
        var inntektsmelding = standardInntektsmelding(LocalDateTime.now());
        when(dokumentArkivTjeneste.hentAlleJournalposter(saksnummer)).thenReturn(
            List.of(søknadMedVedlegg, innteksmeldingJournalpost));
        inntektsmeldingerRepository.lagre(saksnummer, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(saksnummer);

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
        var saksnummer = Saksnummer.dummy();
        var tidspunkt = LocalDateTime.now();
        var inntektsmelding = standardInntektsmelding(tidspunkt);
        var søknadMedVedlegg = søknadMedVedlegg(saksnummer, tidspunkt.plusSeconds(1));
        var vedtak = utgåendeVedtak(saksnummer, tidspunkt.plusSeconds(2));
        when(dokumentArkivTjeneste.hentAlleJournalposter(saksnummer)).thenReturn(List.of(vedtak, søknadMedVedlegg)); // Reversert med vilje
        inntektsmeldingerRepository.lagre(saksnummer, Set.of(inntektsmelding));

        var tidslinje = tjeneste.tidslinje(saksnummer);

        assertThat(tidslinje)
            .hasSize(3)
            .extracting(TidslinjeHendelseDto::tidslinjeHendelseType)
            .containsExactly(
                TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING,
                TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD,
                TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK);
    }

    private static EnkelJournalpost søknadMedVedlegg(Saksnummer saksnummer, LocalDateTime mottatt) {
        return new EnkelJournalpost(
            DokumentTypeId.I000003.getTittel(),
            "1",
            saksnummer.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT, mottatt,
            DokumentTypeId.I000003,
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", DokumentTypeId.I000003.getTittel(), null),
                new EnkelJournalpost.Dokument("2", DokumentTypeId.I000036.getTittel(), null)
            )
        );
    }

    private static EnkelJournalpost endringssøknadUtenVedlegg(Saksnummer saksnummer, LocalDateTime tidspunkt) {
        return new EnkelJournalpost(
            DokumentTypeId.I000050.getTittel(),
            "2",
            saksnummer.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            tidspunkt,
            DokumentTypeId.I000050,
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", DokumentTypeId.I000050.getTittel(), null)
            )
        );
    }

    private static EnkelJournalpost ettersenderVedlegg(Saksnummer saksnummer, LocalDateTime tidspunkt) {
        return new EnkelJournalpost(
            DokumentTypeId.I000036.getTittel(),
            "3",
            saksnummer.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT, tidspunkt,
            DokumentTypeId.I000023,
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", DokumentTypeId.I000023.getTittel(), null),
                new EnkelJournalpost.Dokument("2", DokumentTypeId.I000036.getTittel(), null)
            )
        );
    }

    private static EnkelJournalpost innteksmeldingJournalpost(Saksnummer saksnummer) {
        return new EnkelJournalpost(
            DokumentTypeId.I000067.getTittel(),
            "4",
            saksnummer.value(),
            new EnkelJournalpost.Bruker(Arbeidsgiver.dummy().identifikator(), EnkelJournalpost.Bruker.Type.ORGNR),
            EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            DokumentTypeId.I000067,
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", DokumentTypeId.I000067.getTittel(), null)
            )
        );
    }


    private static EnkelJournalpost utgåendeVedtak(Saksnummer saksnummer, LocalDateTime mottatt) {
        return new EnkelJournalpost(
            "Innvilgelsesbrev foreldrepenger",
            "5",
            saksnummer.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT, mottatt,
            null, // Todo: Dokumentet har vel ikke en hovedtype her? Elller?
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", null, EnkelJournalpost.Brevkode.FORELDREPENGER_INNVILGELSE)
            )
        );
    }


    private static EnkelJournalpost innhentOpplysningsBrev(Saksnummer saksnummer, LocalDateTime tidspunkt) {
        return new EnkelJournalpost(
            "Innhente opplysninger",
            "5",
            saksnummer.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT, tidspunkt,
            null, // Todo: Dokumentet har vel ikke en hovedtype her? Elller?
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", null, EnkelJournalpost.Brevkode.INNHENTE_OPPLYSNINGER)
            )
        );
    }


    private static EnkelJournalpost etterlysIM(Saksnummer saksnummer) {
        return new EnkelJournalpost(
            "Etterlys inntektsmelding",
            "6",
            saksnummer.value(),
            new EnkelJournalpost.Bruker(AktørId.dummy().value(), EnkelJournalpost.Bruker.Type.AKTOERID),
            EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT,
            LocalDateTime.now(),
            null, // Todo: Dokumentet har vel ikke en hovedtype her? Elller?
            EnkelJournalpost.KildeSystem.ANNET,
            List.of(
                new EnkelJournalpost.Dokument("1", null, EnkelJournalpost.Brevkode.ETTERLYS_INNTEKTSMELDING)
            )
        );
    }

    private static Inntektsmelding standardInntektsmelding(LocalDateTime innsendingstidspunkt) {
        return new InntektsmeldingV1("7", Arbeidsgiver.dummy(), innsendingstidspunkt, Beløp.ZERO);
    }
}
