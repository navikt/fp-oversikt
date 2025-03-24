package no.nav.foreldrepenger.oversikt.aareg;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Typer av permisjon og permitteringer.
 * <p>
 * <h3>Kilde: NAV kodeverk</h3>
 * <a href="https://kodeverk.ansatt.nav.no/kodeverk/PermisjonsOgPermitteringsBeskrivelse">Kodeverk</a>
 * <p>
 */
public enum PermType {

    @JsonEnumDefaultValue UKJENT("ukjent"),
    PERMISJON("permisjon"),
    VELFERDSPERMISJON("velferdspermisjon"), // Utgår
    ANNEN_PERMISJON_IKKE_LOVFESTET("andreIkkeLovfestedePermisjoner"),
    ANNEN_PERMISJON_LOVFESTET("andreLovfestedePermisjoner"),
    PERMISJON_VED_MILITÆRTJENESTE("permisjonVedMilitaertjeneste"),

    PERMISJON_MED_FORELDREPENGER("permisjonMedForeldrepenger"),

    UTDANNINGSPERMISJON("utdanningspermisjon"), // Utgår
    UTDANNINGSPERMISJON_IKKE_LOVFESTET("utdanningspermisjonIkkeLovfestet"),
    UTDANNINGSPERMISJON_LOVFESTET("utdanningspermisjonLovfestet"),

    PERMITTERING("permittering"),
    ;

    private static final Set<PermType> ANNEN_PERMISJON = Set.of(PERMISJON, VELFERDSPERMISJON, ANNEN_PERMISJON_IKKE_LOVFESTET,
        ANNEN_PERMISJON_LOVFESTET, PERMISJON_VED_MILITÆRTJENESTE, UKJENT);

    private static final Set<PermType> UTDANNING = Set.of(UTDANNINGSPERMISJON, UTDANNINGSPERMISJON_LOVFESTET, UTDANNINGSPERMISJON_IKKE_LOVFESTET);

    @JsonValue
    private final String offisiellKode;


    PermType(String offisiellKode) {
        this.offisiellKode = offisiellKode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }

    public boolean erAnnenPermisjon() {
        return ANNEN_PERMISJON.contains(this);
    }

    public boolean erUtdanningspermisjon() {
        return UTDANNING.contains(this);
    }

}
