package no.nav.foreldrepenger.oversikt.domene.fp;

public enum Dekningsgrad {
    ÅTTI,
    HUNDRE,
    ;

    public no.nav.foreldrepenger.common.innsyn.DekningsgradSak tilDto() {
        return switch (this) {
            case HUNDRE -> no.nav.foreldrepenger.common.innsyn.DekningsgradSak.HUNDRE;
            case ÅTTI -> no.nav.foreldrepenger.common.innsyn.DekningsgradSak.ÅTTI;
        };
    }
}
