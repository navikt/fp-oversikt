package no.nav.foreldrepenger.oversikt.domene.svp;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;

public record Aktivitet(@JsonProperty("type") Type type,
                        @JsonProperty("arbeidsgiver") Arbeidsgiver arbeidsgiver,
                        @JsonProperty("arbeidsforholdId") String arbeidsforholdId,
                        @JsonProperty("arbeidsgiverNavn") String arbeidsgiverNavn) {
    public no.nav.foreldrepenger.kontrakter.fpoversikt.Aktivitet tilDto() {
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Aktivitet(type.tilDto(), arbeidsgiver == null ? null : arbeidsgiver.tilDto(), arbeidsgiverNavn);
    }

    public enum Type {
        ORDINÆRT_ARBEID,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        FRILANS;

        public no.nav.foreldrepenger.kontrakter.fpoversikt.Aktivitet.AktivitetType tilDto() {
            return switch (this) {
                case ORDINÆRT_ARBEID -> no.nav.foreldrepenger.kontrakter.fpoversikt.Aktivitet.AktivitetType.ORDINÆRT_ARBEID;
                case SELVSTENDIG_NÆRINGSDRIVENDE -> no.nav.foreldrepenger.kontrakter.fpoversikt.Aktivitet.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
                case FRILANS -> no.nav.foreldrepenger.kontrakter.fpoversikt.Aktivitet.AktivitetType.FRILANS;
            };
        }
    }
}
