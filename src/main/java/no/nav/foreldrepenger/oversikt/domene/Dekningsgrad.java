package no.nav.foreldrepenger.oversikt.domene;

public enum Dekningsgrad {
    ÅTTI,
    HUNDRE,
    ;

    public no.nav.foreldrepenger.common.innsyn.Dekningsgrad tilDto() {
        return switch (this) {
            case HUNDRE -> no.nav.foreldrepenger.common.innsyn.Dekningsgrad.HUNDRE;
            case ÅTTI -> no.nav.foreldrepenger.common.innsyn.Dekningsgrad.ÅTTI;
        };
    }
}
