package no.nav.foreldrepenger.oversikt.arbeid;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum PeriodeMedAktivitetskravType {
    @JsonEnumDefaultValue UTTAK,
    UTTAK_FELLESPERIODE,
    UTTAK_BFHR,
    UTSETTELSE,
    UTSETTELSE_BFHR
}
