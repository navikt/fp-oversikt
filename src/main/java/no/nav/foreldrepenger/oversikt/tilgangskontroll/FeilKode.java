package no.nav.foreldrepenger.oversikt.tilgangskontroll;

public enum FeilKode {

    IKKE_TILGANG("Ikke tilgang"),
    IKKE_TILGANG_TIL_DOKUMENT("Dokumentet tilhører ikke bruker i token og/eller ingen fullmakt finnes"),
    IKKE_TILGANG_IKKE_EKSTERN("Bare eksterne kan nå dette endepunktet her"),
    IKKE_TILGANG_UMYNDIG("Innlogget bruker er under myndighetsalder"),
    IKKE_TILGANG_INAKTIV("Innlogget bruker har ingen aktiv personidentifikator"),
    IKKE_TILGANG_MANGLER_DRIFT_ROLLE("Innlogget ansatt har ikke driftrolle");

    private final String beskrivelse;

    FeilKode(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
