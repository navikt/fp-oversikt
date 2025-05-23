package no.nav.foreldrepenger.oversikt.arbeid;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum PeriodeMedAktivitetskravType {
    @JsonEnumDefaultValue UTTAK,
    UTSETTELSE
}
