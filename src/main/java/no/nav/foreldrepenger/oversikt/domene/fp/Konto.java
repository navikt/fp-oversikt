package no.nav.foreldrepenger.oversikt.domene.fp;

public enum Konto {
    FORELDREPENGER,
    MØDREKVOTE,
    FEDREKVOTE,
    FELLESPERIODE,
    FORELDREPENGER_FØR_FØDSEL;

    public no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType tilDto() {
        return switch (this) {
            case FORELDREPENGER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FORELDREPENGER;
            case MØDREKVOTE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.MØDREKVOTE;
            case FEDREKVOTE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FEDREKVOTE;
            case FELLESPERIODE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FELLESPERIODE;
            case FORELDREPENGER_FØR_FØDSEL -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FORELDREPENGER_FØR_FØDSEL;
        };
    }
}

