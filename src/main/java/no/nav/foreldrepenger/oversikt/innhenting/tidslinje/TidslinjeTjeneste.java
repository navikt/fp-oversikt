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
        var alleDokumenterFraSaf = arkivTjeneste.hentAlleJournalposter(saksnummer).stream()
            .filter(journalpost -> !(INNGÅENDE_DOKUMENT.equals(journalpost.type()) && journalpost.hovedtype().erInntektsmelding()))
            .toList();
        var mappedeDokumenter = alleDokumenterFraSaf.stream()
            .map(journalpost -> tilTidslinjeHendelse(journalpost, alleDokumenterFraSaf))
            .flatMap(Optional::stream);
        var mappedeInntektsmeldinger = inntektsmeldingerRepository.hentFor(Set.of(saksnummer)).stream()
            .map(TidslinjeTjeneste::tilTidslinjeHendelse);
        return Stream.concat(mappedeDokumenter, mappedeInntektsmeldinger)
            .sorted()
            .toList();
    }

    private static Optional<TidslinjeHendelseDto> tilTidslinjeHendelse(EnkelJournalpost enkelJournalpost, List<EnkelJournalpost> alleDokumentene) {
        if (enkelJournalpost.type().equals(EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT)) {
            return tidslinjeHendelseTypeUtgåendeDokument(enkelJournalpost)
                .map(hendelseType -> new TidslinjeHendelseDto(
                    enkelJournalpost.mottatt(),
                    enkelJournalpost.journalpostId(),
                    TidslinjeHendelseDto.AktørType.NAV,
                    hendelseType,
                    tilDokumenter(enkelJournalpost.dokumenter())
            ));
        } else if (enkelJournalpost.type().equals(INNGÅENDE_DOKUMENT)) {
            return tidslinjehendelsetype(enkelJournalpost, alleDokumentene)
                .map(hendelseType -> new TidslinjeHendelseDto(
                    enkelJournalpost.mottatt(),
                    enkelJournalpost.journalpostId(),
                    TidslinjeHendelseDto.AktørType.BRUKER,
                    hendelseType,
                    tilDokumenter(enkelJournalpost.dokumenter())
            ));
        }
        throw new IllegalStateException("Utviklerfeil: Noe annet enn utgående eller inngående dokumenter skal ikke mappes og vises til bruker!");
    }

    private static Optional<TidslinjeHendelseDto.TidslinjeHendelseType> tidslinjeHendelseTypeUtgåendeDokument(EnkelJournalpost enkelJournalpost) {
        return switch (enkelJournalpost.dokumenter().stream().findFirst().orElseThrow().brevkode()) { // Alltid bare ett dokument!
            case FORELDREPENGER_ANNULLERT, FORELDREPENGER_AVSLAG, SVANGERSKAPSPENGER_OPPHØR, ENGANGSSTØNAD_INNVILGELSE, SVANGERSKAPSPENGER_AVSLAG,
                FORELDREPENGER_INNVILGELSE, ENGANGSSTØNAD_AVSLAG, FORELDREPENGER_OPPHØR, SVANGERSKAPSPENGER_INNVILGELSE,
                VEDTAK_POSITIVT_OLD, VEDTAK_AVSLAG_OLD, VEDTAK_FORELDREPENGER_OLD, VEDTAK_AVSLAG_FORELDREPENGER_OLD ->
                    Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK);
            case INNHENTE_OPPLYSNINGER, INNHENTE_OPPLYSNINGER_OLD -> Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_INNHENT_OPPLYSNINGER);
            case ETTERLYS_INNTEKTSMELDING, ETTERLYS_INNTEKTSMELDING_OLD -> Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_ETTERLYS_INNTEKTSMELDING);
            default -> {
                LOG.info("Ignorerer utgåpende journalpost med brevkode: {}", enkelJournalpost);
                yield Optional.empty();
            }
        };
    }

    private static Optional<TidslinjeHendelseDto.TidslinjeHendelseType> tidslinjehendelsetype(EnkelJournalpost enkelJournalpost, List<EnkelJournalpost> alleDokumentene) {
        if (enkelJournalpost.hovedtype().erFørstegangssøknad()) {
            return Optional.of(erNyFørstegangssøknad(enkelJournalpost, alleDokumentene) ?
                TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD_NY :
                TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD);
        } else if (enkelJournalpost.hovedtype().erEndringssøknad()) {
            return Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.ENDRINGSSØKNAD);
        } else if (enkelJournalpost.hovedtype().erVedlegg() || enkelJournalpost.hovedtype().erUttalelseOmTilbakekreving()) {
            return  Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.ETTERSENDING);
        } else {
            LOG.info("Ignorer inngående journalpost med dokumenttype: {}", enkelJournalpost);
            return Optional.empty();
        }
    }

    private static boolean erNyFørstegangssøknad(EnkelJournalpost enkelJournalpost, List<EnkelJournalpost> alleDokumentene) {
        return alleDokumentene.stream()
            .filter(j -> INNGÅENDE_DOKUMENT.equals(j.type()))
            .filter(journalpost -> journalpost.hovedtype().erFørstegangssøknad())
            .anyMatch(journalpost -> journalpost.mottatt().isBefore(enkelJournalpost.mottatt()));
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
        return new TidslinjeHendelseDto(
            inntektsmelding.mottattTidspunkt() == null ? inntektsmelding.innsendingstidspunkt() : inntektsmelding.mottattTidspunkt(),
            inntektsmelding.journalpostId(),
            TidslinjeHendelseDto.AktørType.ARBEIDSGIVER,
            TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING,
            List.of()
        );
    }
}
