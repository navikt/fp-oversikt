package no.nav.foreldrepenger.oversikt.tilgangskontroll;

public enum FeilKode {

    IKKE_TILGANG_UMYNDIG("Innlogget bruker er under myndighetsalder");

    private final String beskrivelse;

    FeilKode(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}