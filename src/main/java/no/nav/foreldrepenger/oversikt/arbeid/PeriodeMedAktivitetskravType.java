package no.nav.foreldrepenger.oversikt.arbeid;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum PeriodeMedAktivitetskravType {
    UTTAK_FELLESPERIODE,
    @JsonEnumDefaultValue UTTAK_BFHR, //Frontend skal egentlig ikke sende inn verdi her, men setter default til bfhr siden den er strengest
    UTSETTELSE_BFHR
}
