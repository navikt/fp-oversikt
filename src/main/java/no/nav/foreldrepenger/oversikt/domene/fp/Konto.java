package no.nav.foreldrepenger.oversikt.domene.fp;

public enum Konto {
    FORELDREPENGER,
    MØDREKVOTE,
    FEDREKVOTE,
    FELLESPERIODE,
    FORELDREPENGER_FØR_FØDSEL;

    public no.nav.foreldrepenger.common.innsyn.KontoType tilDto() {
        return switch (this) {
            case FORELDREPENGER -> no.nav.foreldrepenger.common.innsyn.KontoType.FORELDREPENGER;
            case MØDREKVOTE -> no.nav.foreldrepenger.common.innsyn.KontoType.MØDREKVOTE;
            case FEDREKVOTE -> no.nav.foreldrepenger.common.innsyn.KontoType.FEDREKVOTE;
            case FELLESPERIODE -> no.nav.foreldrepenger.common.innsyn.KontoType.FELLESPERIODE;
            case FORELDREPENGER_FØR_FØDSEL -> no.nav.foreldrepenger.common.innsyn.KontoType.FORELDREPENGER_FØR_FØDSEL;
        };
    }
}

