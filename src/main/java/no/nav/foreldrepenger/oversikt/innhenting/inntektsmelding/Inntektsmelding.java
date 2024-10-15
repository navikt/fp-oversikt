package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;

public record Inntektsmelding(BigDecimal inntektPrMnd,
                              BigDecimal refusjonPrMnd,
                          Arbeidsgiver arbeidsgiver,
                          String journalpostId,
                              String kontaktpersonNavn,
                              String kontaktpersonNummer,
                          LocalDateTime innsendingstidspunkt,
                          LocalDateTime mottattTidspunkt,
                          LocalDate startDatoPermisjon,
                          List<NaturalYtelse> aktiveNaturalytelser,
                          List<Refusjon> refusjonsperioder
) {
    public record NaturalYtelse(
        LocalDate fomDato,
        LocalDate tomDato,
        BigDecimal beloepPerMnd,
        String type
    ) {}

    public record Refusjon(
        BigDecimal refusjonsbeløpMnd,
        LocalDate fomDato
    ) {}
}
