package no.nav.foreldrepenger.oversikt.domene.fp;

public enum OppholdÅrsak {
    MØDREKVOTE_ANNEN_FORELDER,
    FEDREKVOTE_ANNEN_FORELDER,
    FELLESPERIODE_ANNEN_FORELDER,
    FORELDREPENGER_ANNEN_FORELDER,
    ;

    public no.nav.foreldrepenger.common.innsyn.UttakOppholdÅrsak tilDto() {
        return switch (this) {
            case MØDREKVOTE_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.UttakOppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
            case FEDREKVOTE_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.UttakOppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
            case FELLESPERIODE_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.UttakOppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
            case FORELDREPENGER_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.UttakOppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
        };
    }
}
