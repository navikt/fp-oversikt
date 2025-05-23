package no.nav.foreldrepenger.oversikt.integrasjoner.digdir;

import java.util.Map;

public record Kontaktinformasjoner(Map<String, Kontaktinformasjon> personer,
                                   Map<String, FeilKode> feil) {

    public record Kontaktinformasjon(boolean aktiv, boolean kanVarsles, boolean reservert, String spraak) {
    }

    public enum FeilKode {
        person_ikke_funnet,
        skjermet,
        fortrolig_adresse,
        strengt_fortrolig_adresse,
        strengt_fortrolig_utenlandsk_adresse,
        noen_andre
    }
}