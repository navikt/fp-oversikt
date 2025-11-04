package no.nav.foreldrepenger.oversikt.domene.fp;

public enum Dekningsgrad {
    ÅTTI,
    HUNDRE,
    ;

    public no.nav.foreldrepenger.kontrakter.fpoversikt.DekningsgradSak tilDto() {
        return switch (this) {
            case HUNDRE -> no.nav.foreldrepenger.kontrakter.fpoversikt.DekningsgradSak.HUNDRE;
            case ÅTTI -> no.nav.foreldrepenger.kontrakter.fpoversikt.DekningsgradSak.ÅTTI;
        };
    }
}
