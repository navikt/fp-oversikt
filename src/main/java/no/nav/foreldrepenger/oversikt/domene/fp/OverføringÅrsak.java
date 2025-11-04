package no.nav.foreldrepenger.oversikt.domene.fp;

public enum OverføringÅrsak {
    INSTITUSJONSOPPHOLD_ANNEN_FORELDER,
    SYKDOM_ANNEN_FORELDER,
    IKKE_RETT_ANNEN_FORELDER,
    ALENEOMSORG,
    ;

    public no.nav.foreldrepenger.kontrakter.felles.kodeverk.Overføringsårsak tilDto() {
        return switch (this) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.Overføringsårsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.Overføringsårsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.Overføringsårsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.Overføringsårsak.ALENEOMSORG;
        };
    }
}
