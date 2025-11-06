package no.nav.foreldrepenger.oversikt.domene.fp;

public enum BrukerRolle {
    MOR, FAR, MEDMOR, UKJENT;

    public no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak tilDto() {
        return switch (this) {
            case MOR -> no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak.MOR;
            case FAR, MEDMOR -> no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak.FAR_MEDMOR;
            case UKJENT -> null;
        };
    }

    public boolean erFarEllerMedmor() {
        return this == FAR || this == MEDMOR;
    }
}
