package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record SøknadsplanPeriode(
    LocalDate fom,
    LocalDate tom,
    Søknadsplan.Rolle rolle,
    KontoType kontoType,
    UtsettelseÅrsak utsettelseÅrsak,
    OverføringÅrsak overføringÅrsak,
    MorsAktivitet morsAktivitet,
    BigDecimal graderingArbeidstidprosent,
    BigDecimal samtidigUttaksprosent,
    Boolean flerbarnsdager,
    AktivitetType aktivitetType,
    String arbeidsgiverId
) {

    public SøknadsplanPeriode {
        Objects.requireNonNull(fom, "fom kan ikke være null");
        Objects.requireNonNull(tom, "tom kan ikke være null");
        Objects.requireNonNull(rolle, "rolle kan ikke være null");
        if (tom.isBefore(fom)) {
            throw new IllegalArgumentException("tom kan ikke være før fom");
        }
    }

    @Override
    public String toString() {
        return "SøknadsplanPeriode[redacted]";
    }

    public enum KontoType {
        FORELDREPENGER,
        MØDREKVOTE,
        FEDREKVOTE,
        FELLESPERIODE,
        FORELDREPENGER_FØR_FØDSEL
    }

    public enum UtsettelseÅrsak {
        HV_ØVELSE,
        ARBEID,
        FERIE,
        SØKER_SYKDOM,
        SØKER_INNLAGT,
        BARN_INNLAGT,
        NAV_TILTAK,
        FRI
    }

    public enum OverføringÅrsak {
        INSTITUSJONSOPPHOLD_ANNEN_FORELDER,
        SYKDOM_ANNEN_FORELDER,
        IKKE_RETT_ANNEN_FORELDER,
        ALENEOMSORG
    }

    public enum MorsAktivitet {
        ARBEID,
        UTDANNING,
        KVALPROG,
        INTROPROG,
        TRENGER_HJELP,
        INNLAGT,
        ARBEID_OG_UTDANNING,
        UFØRE,
        IKKE_OPPGITT
    }

    public enum AktivitetType {
        FRILANS,
        ORDINÆRT_ARBEID,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        ANNET
    }

}
