package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

public enum SNRolleType {

    INNEHAVER("INNH"),
    DELTAKER_PRORATA("DTPR"), // Delt ansvar i ANS,DA,KS
    DELTAKER_SOLIDARISK("DTSO"), // Fullt ansvar i ANS, DA KS
    KOMPLEMENTAR("KOMP"), // KS ubegrenset ansvar
    KONKURS("KENK") // Den personlige konkursen angår
    ;

    private final String kode;

    SNRolleType(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
