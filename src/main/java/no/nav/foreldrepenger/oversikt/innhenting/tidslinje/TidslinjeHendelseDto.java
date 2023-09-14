package no.nav.foreldrepenger.oversikt.innhenting.tidslinje;


import java.time.LocalDateTime;
import java.util.List;

public record TidslinjeHendelseDto(LocalDateTime opprettet,
                                   String journalpostId,
                                   AktørType aktørType,
                                   TidslinjeHendelseType tidslinjeHendelseType,
                                   VedtakType vedtakType, // Unik for vedtak
                                   List<Dokument> dokumenter) implements Comparable<TidslinjeHendelseDto> {

    enum AktørType {
        BRUKER,
        NAV,
        ARBEIDSGIVER
    }

    enum VedtakType {
        INNVILGELSE,
        REVURDERING
    }

    enum TidslinjeHendelseType {
        FØRSTEGANGSSØKNAD,
        FØRSTEGANGSSØKNAD_NY,
        ETTERSENDING,
        ENDRINGSSØKNAD,
        INNTEKTSMELDING,
        VEDTAK,
        VEDTAK_FØRSTEGANG,
        VEDTAK_ENDRING,
        VEDTAK_TILBAKEKREVING,
        VENTER_INNTEKTSMELDING,
        UTGÅENDE_INNHENT_OPPLYSNINGER,
        UTGÅENDE_ETTERLYS_INNTEKTSMELDING
    }

    public record Dokument(String dokumentId, String tittel) {
    }

    @Override
    public int compareTo(TidslinjeHendelseDto o) {
        return opprettet.compareTo(o.opprettet);
    }

}
