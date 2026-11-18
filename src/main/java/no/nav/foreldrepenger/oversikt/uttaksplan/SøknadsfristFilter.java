package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

/**
 * Søknadsfristen for foreldrepenger er tre måneder (folketrygdloven § 22-13 tredje ledd). fpsak avslår uttak før
 * første dag i måneden tre måneder før søknaden ble mottatt. Utsettelser er unntatt.
 *
 * Brukes på søkerens perioder som er hentet fra annen parts søknad. De er bare et forslag til søkeren.
 *
 * Vi legger bevisst inn slakk i forhold til fpsak: grensen er dagens dato minus fire måneder, ikke første dag i
 * måneden tre måneder før, og bare perioder som slutter før grensen fjernes. Grensen ligger dermed alltid før
 * grensen i fpsak. Filteret skal bare skjule uttak som er klart for gamle, ikke gjenskape søknadsfristvurderingen.
 * Uttak nær grensen vises, og fpsak gjør den endelige vurderingen.
 */
final class SøknadsfristFilter {

    // Bevisst slakk: fpsak bruker tre måneder, se klassekommentaren
    private static final Period FRIST_MED_SLAKK = Period.ofMonths(4);

    private SøknadsfristFilter() {
    }

    static LocalDate tidligsteUttaksdato(LocalDate idag) {
        return idag.minus(FRIST_MED_SLAKK);
    }

    /**
     * Fjerner bare uttak som slutter før grensen. Perioder som krysser grensen, beholdes uendret.
     */
    static List<Planperiode> fjernUttakFørFrist(List<Planperiode> perioder, LocalDate idag) {
        var grense = tidligsteUttaksdato(idag);
        return perioder.stream()
            .filter(p -> erUtsettelse(p) || !p.tom().isBefore(grense))
            .toList();
    }

    private static boolean erUtsettelse(Planperiode periode) {
        return periode.uttak().utsettelseÅrsak() != null;
    }
}
