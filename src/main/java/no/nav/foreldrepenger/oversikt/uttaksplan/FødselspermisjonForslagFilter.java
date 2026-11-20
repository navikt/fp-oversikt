package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.fp.VirkedagJusterer;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

/**
 * Forslag i mors søknad om at far eller medmor tar fedrekvote med samtidig uttak fra termin. fpsak flytter mors uttak
 * til fødselsdato, men ikke forslaget.
 */
final class FødselspermisjonForslagFilter {

    private static final BigDecimal HELT_SAMTIDIG_UTTAK = BigDecimal.valueOf(100);
    private static final Set<DayOfWeek> HELG = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private FødselspermisjonForslagFilter() {
    }

    /**
     * Far uten egen sak. Søknadsfrontend spør far om uttak ved fødsel og legger inn ukene selv.
     */
    static List<Planperiode> fjernForslagFraTermin(List<Planperiode> farsPerioder, FamilieHendelse familieHendelse) {
        var fraTermin = fraTermin(familieHendelse);
        if (fraTermin.isEmpty()) {
            return farsPerioder;
        }
        return farsPerioder.stream()
            .filter(periode -> !erForslagFraTermin(periode, fraTermin.get()))
            .toList();
    }

    /**
     * Mor ser forslaget fra sin egen søknad. Det flyttes til mors første periode med 100 % samtidig uttak hvis den har like
     * mange uttaksdager, ellers fjernes det.
     */
    static List<Planperiode> flyttForslagTilMorsSamtidigeUttak(List<Planperiode> farsPerioder, List<Planperiode> morsPerioder,
                                                               FamilieHendelse familieHendelse) {
        var fraTermin = fraTermin(familieHendelse);
        if (fraTermin.isEmpty()) {
            return farsPerioder;
        }
        var morsSamtidigeUttak = morsPerioder.stream()
            .filter(FødselspermisjonForslagFilter::harHeltSamtidigUttak)
            .min(Comparator.comparing(Planperiode::fom));
        return farsPerioder.stream()
            .flatMap(periode -> erForslagFraTermin(periode, fraTermin.get())
                ? morsSamtidigeUttak.flatMap(mor -> flytt(periode, mor, farsPerioder)).stream()
                : Stream.of(periode))
            .sorted(Comparator.comparing(Planperiode::fom))
            .toList();
    }

    private static Optional<Planperiode> flytt(Planperiode forslag, Planperiode morsSamtidigeUttak, List<Planperiode> farsPerioder) {
        if (uttaksdager(forslag) != uttaksdager(morsSamtidigeUttak)) {
            return Optional.empty();
        }
        var flyttet = new Planperiode(morsSamtidigeUttak.fom(), morsSamtidigeUttak.tom(), forslag.uttak());
        // Overlapp gir feil i tidslinjen
        var overlapperAndrePerioder = farsPerioder.stream()
            .filter(periode -> periode != forslag)
            .anyMatch(periode -> !periode.fom().isAfter(flyttet.tom()) && !periode.tom().isBefore(flyttet.fom()));
        return overlapperAndrePerioder ? Optional.empty() : Optional.of(flyttet);
    }

    private static Optional<LocalDate> fraTermin(FamilieHendelse familieHendelse) {
        if (familieHendelse == null || familieHendelse.gjelderAdopsjon() || familieHendelse.termindato() == null) {
            return Optional.empty();
        }
        // UttaksplanMapper flytter fom fra helg til mandag
        return Optional.of(VirkedagJusterer.justerFom(familieHendelse.termindato()));
    }

    private static boolean erForslagFraTermin(Planperiode periode, LocalDate fraTermin) {
        var uttak = periode.uttak();
        return periode.fom().equals(fraTermin)
            && uttak.forelder() == FellesUttaksplanDto.Rolle.FAR_MEDMOR
            && uttak.kontoType() == KontoType.FEDREKVOTE
            && uttak.resultat() == null
            && uttak.samtidigUttak() != null
            && uttak.samtidigUttak().value().compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean harHeltSamtidigUttak(Planperiode periode) {
        var samtidigUttak = periode.uttak().samtidigUttak();
        return samtidigUttak != null && samtidigUttak.value().compareTo(HELT_SAMTIDIG_UTTAK) >= 0;
    }

    private static long uttaksdager(Planperiode periode) {
        return periode.fom().datesUntil(periode.tom().plusDays(1))
            .filter(dato -> !HELG.contains(dato.getDayOfWeek()))
            .count();
    }
}
