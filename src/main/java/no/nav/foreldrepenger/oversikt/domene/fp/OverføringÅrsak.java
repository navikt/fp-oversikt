package no.nav.foreldrepenger.oversikt.domene.fp;

public enum OverføringÅrsak {
    INSTITUSJONSOPPHOLD_ANNEN_FORELDER,
    SYKDOM_ANNEN_FORELDER,
    IKKE_RETT_ANNEN_FORELDER,
    ALENEOMSORG,
    ;

    public no.nav.foreldrepenger.common.innsyn.UttakOverføringÅrsak tilDto() {
        return switch (this) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.UttakOverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.UttakOverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> no.nav.foreldrepenger.common.innsyn.UttakOverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> no.nav.foreldrepenger.common.innsyn.UttakOverføringÅrsak.ALENEOMSORG;
        };
    }
}
