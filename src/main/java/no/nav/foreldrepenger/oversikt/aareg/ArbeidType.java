package no.nav.foreldrepenger.oversikt.aareg;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Typer av arbeidsforhold.
 * <p>
 * <h3>Kilde: NAV kodeverk</h3>
 * <a href="https://kodeverk.ansatt.nav.no/kodeverk/Arbeidsforholdstyper">Kodeverk</a>
 * <p>
 */
public enum ArbeidType {

    @JsonEnumDefaultValue UKJENT("ukjent"),
    FORENKLET_OPPGJØRSORDNING("forenkletOppgjoersordning"),
    FRILANSER_OPPDRAGSTAKER_MED_MER("frilanserOppdragstakerHonorarPersonerMm"),
    MARITIMT_ARBEIDSFORHOLD("maritimtArbeidsforhold"),
    ORDINÆRT_ARBEIDSFORHOLD("ordinaertArbeidsforhold"),
    PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD("pensjonOgAndreTyperYtelserUtenAnsettelsesforhold"),
    ;


    @JsonValue
    private final String offisiellKode;


    ArbeidType(String offisiellKode) {
        this.offisiellKode = offisiellKode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }

}
