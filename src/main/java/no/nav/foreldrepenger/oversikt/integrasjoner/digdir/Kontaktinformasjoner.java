package no.nav.foreldrepenger.oversikt.integrasjoner.digdir;

import no.nav.foreldrepenger.common.oppslag.dkif.Målform;

import java.util.Map;

public record Kontaktinformasjoner(Map<String, Kontaktinformasjon> personer, Map<String, FeilKode> feil) {

    public record Kontaktinformasjon(boolean aktiv, Målform spraak) {
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