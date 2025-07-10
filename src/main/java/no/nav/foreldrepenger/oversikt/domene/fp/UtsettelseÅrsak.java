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

    public no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak tilDto() {
        return switch (this) {
            case HV_ØVELSE -> no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.HV_ØVELSE;
            case ARBEID -> no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.ARBEID;
            case LOVBESTEMT_FERIE -> no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.LOVBESTEMT_FERIE;
            case SØKER_SYKDOM -> no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.BARN_INNLAGT;
            case NAV_TILTAK -> no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.NAV_TILTAK;
            case FRI -> no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.FRI;
        };
    }
}
