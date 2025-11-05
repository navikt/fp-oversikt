package no.nav.foreldrepenger.oversikt.tidslinje;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.kontrakter.fpoversikt.inntektsmelding.FpOversiktInntektsmeldingDto;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpostSelvbetjening;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostType;
import no.nav.foreldrepenger.oversikt.arkiv.SafSelvbetjeningTjeneste;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.InntektsmeldingTjeneste;


@ApplicationScoped
public class TidslinjeTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(TidslinjeTjeneste.class);
    private static final String VARSEL_TILBAKEBETALING_TITTEL = "Varsel tilbakebetaling";

    private SafSelvbetjeningTjeneste safSelvbetjeningTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    public TidslinjeTjeneste() {
        // CDI
    }

    @Inject
    public TidslinjeTjeneste(SafSelvbetjeningTjeneste safSelvbetjeningTjeneste, InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.safSelvbetjeningTjeneste = safSelvbetjeningTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }


    public List<TidslinjeHendelseDto> tidslinje(Fødselsnummer fødselsnummer, Saksnummer saksnummer) {
        var alleDokumenterFraSaf = safSelvbetjeningTjeneste.alleJournalposter(fødselsnummer, saksnummer).stream()
            .filter(journalpost -> !(JournalpostType.INNGÅENDE_DOKUMENT.equals(journalpost.type()) && journalpost.hovedtype() != null && journalpost.hovedtype().erInntektsmelding()))
            .toList();
        var mappedeDokumenter = alleDokumenterFraSaf.stream()
            .map(journalpost -> tilTidslinjeHendelse(journalpost, alleDokumenterFraSaf))
            .flatMap(Optional::stream);
        var mappedeInntektsmeldinger = inntektsmeldingTjeneste.inntektsmeldinger(saksnummer).stream()
            .map(TidslinjeTjeneste::tilTidslinjeHendelse);
        return Stream.concat(mappedeDokumenter, mappedeInntektsmeldinger)
            .sorted(Comparator.comparing(TidslinjeHendelseDto::opprettet))
            .toList();
    }

    private static Optional<TidslinjeHendelseDto> tilTidslinjeHendelse(EnkelJournalpostSelvbetjening enkelJournalpost, List<EnkelJournalpostSelvbetjening> alleDokumentene) {
        if (enkelJournalpost.type().equals(JournalpostType.UTGÅENDE_DOKUMENT)) {
            return tidslinjeHendelseTypeUtgåendeDokument(enkelJournalpost)
                .map(hendelseType -> new TidslinjeHendelseDto(
                    enkelJournalpost.mottatt(),
                    TidslinjeHendelseDto.AktørType.NAV,
                    hendelseType,
                    tilDokumenter(enkelJournalpost.dokumenter(), enkelJournalpost.journalpostId())
                ));
        } else if (enkelJournalpost.type().equals(JournalpostType.INNGÅENDE_DOKUMENT)) {
            return tidslinjehendelsetype(enkelJournalpost, alleDokumentene)
                .map(hendelseType -> new TidslinjeHendelseDto(
                    enkelJournalpost.mottatt(),
                    TidslinjeHendelseDto.AktørType.BRUKER,
                    hendelseType,
                    tilDokumenter(enkelJournalpost.dokumenter(), enkelJournalpost.journalpostId())
                ));
        }
        throw new IllegalStateException("Utviklerfeil: Noe annet enn utgående eller inngående dokumenter skal ikke mappes og vises til bruker!");
    }

    private static Optional<TidslinjeHendelseDto.TidslinjeHendelseType> tidslinjeHendelseTypeUtgåendeDokument(EnkelJournalpostSelvbetjening enkelJournalpost) {
        var brevkode = enkelJournalpost.dokumenter().stream().findFirst().orElseThrow().brevkode();
        if (brevkode.erVedtak()) {
            return Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK);
        } else if (brevkode.erInnhentOpplysninger()) {
            return Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_INNHENT_OPPLYSNINGER);
        } else if (brevkode.erEtterlysIM()) {
            return Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_ETTERLYS_INNTEKTSMELDING);
        } else if (brevkode.erVarselOmTilbakebetaling() && enkelJournalpost.tittel().contains(VARSEL_TILBAKEBETALING_TITTEL)) {
            LOG.info("Varsel om tilbakebetaling returnes {}", enkelJournalpost);
            return Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.UTGÅENDE_VARSEL_TILBAKEBETALING);
        } else if (brevkode.equals(EnkelJournalpostSelvbetjening.Brevkode.FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_INFOBREV)
                || brevkode.equals(EnkelJournalpostSelvbetjening.Brevkode.FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_FORLENGET_SAKSBEHANDLINGSTID)) {
            return Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_INFOBREV);
        } else {
            LOG.info("Ignorerer utgåpende journalpost: {}", enkelJournalpost);
            return Optional.empty();
        }
    }

    private static Optional<TidslinjeHendelseDto.TidslinjeHendelseType> tidslinjehendelsetype(EnkelJournalpostSelvbetjening enkelJournalpost, List<EnkelJournalpostSelvbetjening> alleDokumentene) {
        var dokumentType = enkelJournalpost.hovedtype();
        if (dokumentType == null) {
            LOG.info("Ignorer inngående journalpost med dokumenttype: {}", enkelJournalpost);
            return Optional.empty();
        }
        if (dokumentType.erFørstegangssøknad()) {
            return Optional.of(erNyFørstegangssøknad(enkelJournalpost, alleDokumentene) ?
                TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD_NY :
                TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD);
        } else if (dokumentType.erEndringssøknad()) {
            return Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.ENDRINGSSØKNAD);
        } else if (dokumentType.erVedlegg() || dokumentType.erUttalelseOmTilbakekreving()) {
            return  Optional.of(TidslinjeHendelseDto.TidslinjeHendelseType.ETTERSENDING);
        } else {
            LOG.info("Ignorer inngående journalpost med dokumenttype: {}", enkelJournalpost);
            return Optional.empty();
        }
    }

    private static boolean erNyFørstegangssøknad(EnkelJournalpostSelvbetjening enkelJournalpost, List<EnkelJournalpostSelvbetjening> alleDokumentene) {
        return alleDokumentene.stream()
            .filter(j -> JournalpostType.INNGÅENDE_DOKUMENT.equals(j.type()))
            .filter(journalpost -> journalpost.hovedtype() != null && journalpost.hovedtype().erFørstegangssøknad())
            .anyMatch(journalpost -> journalpost.mottatt().isBefore(enkelJournalpost.mottatt()));
    }

    private static List<TidslinjeHendelseDto.Dokument> tilDokumenter(List<EnkelJournalpostSelvbetjening.Dokument> dokumenter, String journalpostId) {
        return dokumenter.stream()
            .map(dokument -> tilDokument(dokument, journalpostId))
            .toList();
    }

    private static TidslinjeHendelseDto.Dokument tilDokument(EnkelJournalpostSelvbetjening.Dokument dokument, String journalpostId) {
        return new TidslinjeHendelseDto.Dokument(journalpostId, dokument.dokumentId(), dokument.tittel());
    }

    private static TidslinjeHendelseDto tilTidslinjeHendelse(FpOversiktInntektsmeldingDto inntektsmelding) {
        // versjon 1 har ikke journalPostId.
        var dokumenter = inntektsmelding.versjon() == 2
                ? List.of(new TidslinjeHendelseDto.Dokument(inntektsmelding.journalpostId(), null, ""))
                : List.<TidslinjeHendelseDto.Dokument>of();

        return new TidslinjeHendelseDto(
            inntektsmelding.mottattTidspunkt(),
            TidslinjeHendelseDto.AktørType.ARBEIDSGIVER,
            TidslinjeHendelseDto.TidslinjeHendelseType.INNTEKTSMELDING,
            dokumenter
        );
    }
}
