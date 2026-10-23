package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;

public record UttakAktivitet(Type type, Arbeidsgiver arbeidsgiver, String arbeidsforholdId) {
    public enum Type {
        ORDINÆRT_ARBEID,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        FRILANS,
        ANNET
    }
}
