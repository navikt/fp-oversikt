package no.nav.foreldrepenger.oversikt.domene.fp;

import java.time.LocalDate;


public final class VirkedagJusterer {

    private VirkedagJusterer() {
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
}
