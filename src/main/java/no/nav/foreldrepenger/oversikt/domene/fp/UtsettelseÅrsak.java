package no.nav.foreldrepenger.oversikt.domene.fp;

public enum UtsettelseÅrsak {
    HV_ØVELSE,
    ARBEID,
    LOVBESTEMT_FERIE,
    SØKER_SYKDOM,
    SØKER_INNLAGT,
    BARN_INNLAGT,
    NAV_TILTAK,
    FRI;

    public no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak tilDto() {
        return switch (this) {
            case HV_ØVELSE -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak.HV_ØVELSE;
            case ARBEID -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak.ARBEID;
            case LOVBESTEMT_FERIE -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak.LOVBESTEMT_FERIE;
            case SØKER_SYKDOM -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak.BARN_INNLAGT;
            case NAV_TILTAK -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak.NAV_TILTAK;
            case FRI -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakUtsettelseÅrsak.FRI;
        };
    }
}
