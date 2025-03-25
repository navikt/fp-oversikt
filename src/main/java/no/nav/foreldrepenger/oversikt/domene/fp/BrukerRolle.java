package no.nav.foreldrepenger.oversikt.domene.fp;

public enum BrukerRolle {
    MOR, FAR, MEDMOR, UKJENT;

    public no.nav.foreldrepenger.common.innsyn.BrukerRolle tilDto() {
        return switch (this) {
            case MOR -> no.nav.foreldrepenger.common.innsyn.BrukerRolle.MOR;
            case FAR, MEDMOR -> no.nav.foreldrepenger.common.innsyn.BrukerRolle.FAR_MEDMOR;
            case UKJENT -> null;
        };
    }

    public boolean erFarEllerMedmor() {
        return this == FAR || this == MEDMOR;
    }
}
