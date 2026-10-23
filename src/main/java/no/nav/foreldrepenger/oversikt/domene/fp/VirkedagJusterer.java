package no.nav.foreldrepenger.oversikt.domene.fp;

import java.time.LocalDate;
import java.util.List;


public final class VirkedagJusterer {

    private VirkedagJusterer() {
    }

    public static List<no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs> justerEøsPerioder(
        List<no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs> perioder) {
        return perioder.stream()
            .map(VirkedagJusterer::justerEøsPeriode)
            .filter(p -> !p.fom().isAfter(p.tom()))
            .toList();
    }

    public static LocalDate justerFom(LocalDate dato) {
        return switch (dato.getDayOfWeek()) {
            case SATURDAY -> dato.plusDays(2);
            case SUNDAY -> dato.plusDays(1);
            default -> dato;
        };
    }

    public static LocalDate justerTom(LocalDate dato) {
        return switch (dato.getDayOfWeek()) {
            case SATURDAY -> dato.minusDays(1);
            case SUNDAY -> dato.minusDays(2);
            default -> dato;
        };
    }

    private static no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs justerEøsPeriode(
        no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs p) {
        var fom = justerFom(p.fom());
        var tom = justerTom(p.tom());
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeAnnenpartEøs(fom, tom, p.kontoType(), p.trekkdager());
    }
}
