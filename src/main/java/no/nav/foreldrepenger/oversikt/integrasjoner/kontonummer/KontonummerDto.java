package no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer;

public record KontonummerDto(String kontonummer, UtenlandskKontoInfo utenlandskKontoInfo) {
    public static final KontonummerDto UKJENT = new KontonummerDto(null, null);

    public record UtenlandskKontoInfo(String banknavn,
                                      String bankkode,
                                      String bankLandkode,
                                      String valutakode,
                                      String swiftBicKode,
                                      String bankadresse1,
                                      String bankadresse2,
                                      String bankadresse3) {
    }

}
