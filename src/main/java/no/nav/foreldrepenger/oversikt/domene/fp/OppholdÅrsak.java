package no.nav.foreldrepenger.oversikt.domene.fp;

public enum OppholdÅrsak {
    MØDREKVOTE_ANNEN_FORELDER,
    FEDREKVOTE_ANNEN_FORELDER,
    FELLESPERIODE_ANNEN_FORELDER,
    FORELDREPENGER_ANNEN_FORELDER,
    ;

    public no.nav.foreldrepenger.kontrakter.fpoversikt.UttakOppholdÅrsak tilDto() {
        return switch (this) {
            case MØDREKVOTE_ANNEN_FORELDER -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakOppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
            case FEDREKVOTE_ANNEN_FORELDER -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakOppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
            case FELLESPERIODE_ANNEN_FORELDER -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakOppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
            case FORELDREPENGER_ANNEN_FORELDER -> no.nav.foreldrepenger.kontrakter.fpoversikt.UttakOppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
        };
    }
}
