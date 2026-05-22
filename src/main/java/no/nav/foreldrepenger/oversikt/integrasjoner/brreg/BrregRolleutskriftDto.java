package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

import java.util.List;

public record BrregRolleutskriftDto(List<EnhetDto> enheter) {

    public record EnhetDto(String organisasjonsnummer, String navn, List<RolleDto> roller, LinksDto _links) {
    }

    public record RolleDto(Boolean fratraadt, Boolean avregistrert, RolleKodeDto type) {
    }

    public record RolleKodeDto(String kode, String beskrivelse) {
    }

    public record LinksDto(LinkDto enhet) {
    }

    public record LinkDto(String href) {
    }


}
