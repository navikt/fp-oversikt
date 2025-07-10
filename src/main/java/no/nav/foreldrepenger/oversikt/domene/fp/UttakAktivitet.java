package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.common.innsyn.Aktivitet;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;

public record UttakAktivitet(Type type, Arbeidsgiver arbeidsgiver, String arbeidsforholdId) {
    public enum Type {
        ORDINÆRT_ARBEID,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        FRILANS,
        ANNET;

        public Aktivitet.AktivitetType tilDto() {
            return switch (this) {

                case ORDINÆRT_ARBEID -> Aktivitet.AktivitetType.ORDINÆRT_ARBEID;
                case SELVSTENDIG_NÆRINGSDRIVENDE -> Aktivitet.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
                case FRILANS -> Aktivitet.AktivitetType.FRILANS;
                case ANNET -> Aktivitet.AktivitetType.ANNET;
            };
        }
    }
}
