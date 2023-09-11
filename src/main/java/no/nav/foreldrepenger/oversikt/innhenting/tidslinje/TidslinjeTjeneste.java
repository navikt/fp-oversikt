package no.nav.foreldrepenger.oversikt.innhenting.tidslinje;

import static no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpost.DokumentType.INNGÅENDE_DOKUMENT;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
            .map(TidslinjeTjeneste::tilTidslinjeHendelse);
        var inntektsmeldinger = inntektsmeldingerRepository.hentFor(Set.of(saksnummer)).stream()
            .map(TidslinjeTjeneste::tilTidslinjeHendelse);
        return Stream.concat(dokumenter, inntektsmeldinger)
            .sorted()
            .toList();
    }

    private static TidslinjeHendelseDto tilTidslinjeHendelse(EnkelJournalpost enkelJournalpost) {
        if (enkelJournalpost.type().equals(EnkelJournalpost.DokumentType.UTGÅENDE_DOKUMENT)) {
            return switch (enkelJournalpost.dokumenter().stream().findFirst().orElseThrow().brevkode()) { // Alltid bare ett dokument!
                case ANUFOR, AVSFOR, OPPSVP, INNVES, AVSSVP, INVFOR, AVSLES, OPPFOR, INVSVP -> vedtakshendelse(enkelJournalpost);
                case INNOPP -> innhentOpplysningsBrev(enkelJournalpost);
                case ELYSIM -> etterlysInntektsmelding(enkelJournalpost);
                default -> throw new IllegalStateException("Ukjent brevkode. What to do?"); // TODO: Skal vel ikke være mulig å ikke ha brevkode for utgående dokumenter?
            };
        } else if (enkelJournalpost.type().equals(INNGÅENDE_DOKUMENT)) {
            if (enkelJournalpost.hovedtype().erFørstegangssøknad() || enkelJournalpost.hovedtype().erEndringssøknad()) {
                return new TidslinjeHendelseDto(
                    enkelJournalpost.mottatt(),
                    enkelJournalpost.journalpostId(),
                    TidslinjeHendelseDto.AktørType.BRUKER,
                    enkelJournalpost.hovedtype().erFørstegangssøknad() ? TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD : TidslinjeHendelseDto.TidslinjeHendelseType.ENDRINGSSØKNAD,
                    null,
                    null,
                    tilDokumenter(enkelJournalpost.dokumenter())
                );

            } else if (enkelJournalpost.hovedtype().erVedlegg()) {
                return new TidslinjeHendelseDto(
                    enkelJournalpost.mottatt(),
                    enkelJournalpost.journalpostId(),
                    TidslinjeHendelseDto.AktørType.BRUKER,
                    TidslinjeHendelseDto.TidslinjeHendelseType.ETTERSENDING,
                    null,
                    null,
                    tilDokumenter(enkelJournalpost.dokumenter())
                );
            } else {
                throw new IllegalStateException("Utviklerfeil: Hentet en journalpost av typen INNGÅENDE_DOKUMENT med ukjent dokumenttype " + enkelJournalpost.hovedtype());
            }
        }
        throw new IllegalStateException("Utviklerfeil: Noe annet enn utgående eller inngående dokumenter skal ikke mappes og vises til bruker!");
    }

    private static TidslinjeHendelseDto etterlysInntektsmelding(EnkelJournalpost enkelJournalpost) {
        return new TidslinjeHendelseDto(
            enkelJournalpost.mottatt(),
            enkelJournalpost.journalpostId(),
            TidslinjeHendelseDto.AktørType.NAV,
            TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_ETTERLYS_INNTEKTSMELDING,
            null,
            null,
            tilDokumenter(enkelJournalpost.dokumenter())
        );
    }

    private static TidslinjeHendelseDto innhentOpplysningsBrev(EnkelJournalpost enkelJournalpost) {
        return new TidslinjeHendelseDto(
            enkelJournalpost.mottatt(),
            enkelJournalpost.journalpostId(),
            TidslinjeHendelseDto.AktørType.NAV,
            TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_INNHENT_OPPLYSNINGER,
            null,
            null,
            tilDokumenter(enkelJournalpost.dokumenter())
        );
    }

    private static TidslinjeHendelseDto vedtakshendelse(EnkelJournalpost enkelJournalpost) {
        return new TidslinjeHendelseDto(
            enkelJournalpost.mottatt(),
            enkelJournalpost.journalpostId(),
            TidslinjeHendelseDto.AktørType.NAV,
            TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK,
            TidslinjeHendelseDto.VedtakType.INNVILGELSE,
            null,
            tilDokumenter(enkelJournalpost.dokumenter())
        );
    }

    private static List<TidslinjeHendelseDto.Dokument> tilDokumenter(List<EnkelJournalpost.Dokument> dokumenter) {
        return dokumenter.stream()
            .map(TidslinjeTjeneste::tilDokument)
            .toList();
    }

    private static TidslinjeHendelseDto.Dokument tilDokument(EnkelJournalpost.Dokument dokument) {
        var dokumentTypeId = dokument.dokumentTypeId() != null ? dokument.dokumentTypeId().name() : null;
        return new TidslinjeHendelseDto.Dokument(dokument.dokumentId(), dokumentTypeId);
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
            new Arbeidsgiver(inntektsmelding.arbeidsgiver().identifikator(), arbeidsgiverType),
            null
        );
    }


}
