package no.nav.foreldrepenger.oversikt.innhenting.tidslinje;

import static no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.innsyn.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;

@ApplicationScoped
public class TidslinjeTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(TidslinjeTjeneste.class);
    private DokumentArkivTjeneste arkivTjeneste;
    private InntektsmeldingerRepository inntektsmeldingerRepository;

    @Inject
    public TidslinjeTjeneste(DokumentArkivTjeneste arkivTjeneste, InntektsmeldingerRepository inntektsmeldingerRepository) {
        this.arkivTjeneste = arkivTjeneste;
        this.inntektsmeldingerRepository = inntektsmeldingerRepository;
    }

    TidslinjeTjeneste() {
        // CDI
    }

    public List<TidslinjeHendelseDto> tidslinje(Saksnummer saksnummer) {
        var dokumenter = arkivTjeneste.hentAlleJournalposter(saksnummer).stream()
            .filter(journalpost -> !(INNGÅENDE_DOKUMENT.equals(journalpost.type()) && journalpost.hovedtype().erInntektsmelding()))
            .map(TidslinjeTjeneste::tilTidslinjeHendelse)
            .flatMap(Optional::stream);
        var inntektsmeldinger = inntektsmeldingerRepository.hentFor(Set.of(saksnummer)).stream()
            .map(TidslinjeTjeneste::tilTidslinjeHendelse);
        return Stream.concat(dokumenter, inntektsmeldinger)
            .sorted()
            .toList();
    }

    private static Optional<TidslinjeHendelseDto> tilTidslinjeHendelse(EnkelJournalpost enkelJournalpost) {
        if (enkelJournalpost.type().equals(EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT)) {
            return switch (enkelJournalpost.dokumenter().stream().findFirst().orElseThrow().brevkode()) { // Alltid bare ett dokument!
                case FORELDREPENGER_ANNULLERT, FORELDREPENGER_AVSLAG, SVANGERSKAPSPENGER_OPPHØR, ENGANGSSTØNAD_INNVILGELSE, SVANGERSKAPSPENGER_AVSLAG, FORELDREPENGER_INNVILGELSE, ENGANGSSTØNAD_AVSLAG, FORELDREPENGER_OPPHØR, SVANGERSKAPSPENGER_INNVILGELSE -> vedtakshendelse(enkelJournalpost);
                case INNHENTE_OPPLYSNINGER -> innhentOpplysningsBrev(enkelJournalpost);
                case ETTERLYS_INNTEKTSMELDING -> etterlysInntektsmelding(enkelJournalpost);
                default -> {
                    LOG.info("Journalpost med ukjent brevkode: {}", enkelJournalpost);
                    yield Optional.empty();
                }
            };
        } else if (enkelJournalpost.type().equals(INNGÅENDE_DOKUMENT)) {
            if (enkelJournalpost.hovedtype().erFørstegangssøknad() || enkelJournalpost.hovedtype().erEndringssøknad()) {
                return Optional.of(new TidslinjeHendelseDto(
                    enkelJournalpost.mottatt(),
                    enkelJournalpost.journalpostId(),
                    TidslinjeHendelseDto.AktørType.BRUKER,
                    enkelJournalpost.hovedtype().erFørstegangssøknad() ? TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD : TidslinjeHendelseDto.TidslinjeHendelseType.ENDRINGSSØKNAD,
                    null,
                    tilDokumenter(enkelJournalpost.dokumenter())
                ));

            } else if (enkelJournalpost.hovedtype().erVedlegg()) {
                return Optional.of(new TidslinjeHendelseDto(
                    enkelJournalpost.mottatt(),
                    enkelJournalpost.journalpostId(),
                    TidslinjeHendelseDto.AktørType.BRUKER,
                    TidslinjeHendelseDto.TidslinjeHendelseType.ETTERSENDING,
                    null,
                    tilDokumenter(enkelJournalpost.dokumenter())
                ));
            } else {
                LOG.info("Utviklerfeil: Hentet en journalpost av typen INNGÅENDE_DOKUMENT med ukjent dokumenttype: {}", enkelJournalpost);
                return Optional.empty();
            }
        }
        throw new IllegalStateException("Utviklerfeil: Noe annet enn utgående eller inngående dokumenter skal ikke mappes og vises til bruker!");
    }

    private static Optional<TidslinjeHendelseDto> etterlysInntektsmelding(EnkelJournalpost enkelJournalpost) {
        return Optional.of(new TidslinjeHendelseDto(
            enkelJournalpost.mottatt(),
            enkelJournalpost.journalpostId(),
            TidslinjeHendelseDto.AktørType.NAV,
            TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_ETTERLYS_INNTEKTSMELDING,
            null,
            tilDokumenter(enkelJournalpost.dokumenter())
        ));
    }

    private static Optional<TidslinjeHendelseDto> innhentOpplysningsBrev(EnkelJournalpost enkelJournalpost) {
        return Optional.of(new TidslinjeHendelseDto(
            enkelJournalpost.mottatt(),
            enkelJournalpost.journalpostId(),
            TidslinjeHendelseDto.AktørType.NAV,
            TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_INNHENT_OPPLYSNINGER,
            null,
            tilDokumenter(enkelJournalpost.dokumenter())
        ));
    }

    private static Optional<TidslinjeHendelseDto> vedtakshendelse(EnkelJournalpost enkelJournalpost) {
        return Optional.of(new TidslinjeHendelseDto(
            enkelJournalpost.mottatt(),
            enkelJournalpost.journalpostId(),
            TidslinjeHendelseDto.AktørType.NAV,
            TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK,
            TidslinjeHendelseDto.VedtakType.INNVILGELSE,
            tilDokumenter(enkelJournalpost.dokumenter())
        ));
    }

    private static List<TidslinjeHendelseDto.Dokument> tilDokumenter(List<EnkelJournalpost.Dokument> dokumenter) {
        return dokumenter.stream()
            .map(TidslinjeTjeneste::tilDokument)
            .toList();
    }

    private static TidslinjeHendelseDto.Dokument tilDokument(EnkelJournalpost.Dokument dokument) {
        return new TidslinjeHendelseDto.Dokument(dokument.dokumentId(), dokument.tittel());
    }

    private static TidslinjeHendelseDto tilTidslinjeHendelse(Inntektsmelding inntektsmelding) {
        var arbeidsgiverType = inntektsmelding.arbeidsgiver().identifikator().length() != 11
            ? Arbeidsgiver.ArbeidsgiverType.ORGANISASJON
            : Arbeidsgiver.ArbeidsgiverType.PRIVAT;
        return new TidslinjeHendelseDto(
            inntektsmelding.innsendingstidspunkt(),
            inntektsmelding.journalpostId(),
            TidslinjeHendelseDto.AktørType.ARBEIDSGIVER,
            TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING,
            null,
            List.of()
        );
    }


}
