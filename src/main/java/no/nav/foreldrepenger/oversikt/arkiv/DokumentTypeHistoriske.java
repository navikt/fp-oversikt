package no.nav.foreldrepenger.oversikt.arkiv;

import java.util.Arrays;
import java.util.Set;


/**
 * Avviker fra DokumentType ved innsending, siden denne inneholder historiske dokumenttyper
 */
public enum DokumentTypeHistoriske {
    I000001("Søknad om svangerskapspenger"),
    I000002("Søknad om foreldrepenger ved adopsjon"),
    I000003("Søknad om engangsstønad ved fødsel"),
    I000004("Søknad om engangsstønad ved adopsjon"),
    I000005("Søknad om foreldrepenger ved fødsel"),
    I000006("Utsettelse eller gradert uttak av foreldrepenger (fleksibelt uttak)"),
    I000050("Søknad om endring av uttak av foreldrepenger eller overføring av kvote"),

    // Klage
    I000027("Klage/anke"),
    I500027("Ettersendelse til klage/anke"),

    // Tilbakekreving
    I000114("Uttalelse tilbakekreving"),
    I000119("Uttalelse om tilbakebetaling"),

    // Inntektsmelding
    I000067("Inntektsmelding"),

    // Vedlegg/Dokumentasjon
    I000007("Inntektsopplysninger om selvstendig næringsdrivende og/eller frilansere som skal ha foreldrepenger eller svangerskapspenger"),
    I000023("Legeerklæring"),
    I000026("Inntektsopplysninger for arbeidstaker som skal ha sykepenger, foreldrepenger, svangerskapspenger, pleie-/opplæringspenger"),
    I000032("Resultatregnskap"),
    I000036("Dokumentasjon av ferie"),
    I000037("Dokumentasjon av innleggelse i helseinstitusjon"),
    I000038("Dokumentasjon av mors utdanning, arbeid eller sykdom"),
    I000039("Dokumentasjon av militær- eller siviltjeneste"),
    I000041("Dokumentasjon av termindato (lev. kun av mor), fødsel eller dato for omsorgsovertakelse"),
    I000042("Dokumentasjon av dato for overtakelse av omsorg"),
    I000043("Dokumentasjon av arbeidsforhold"),
    I000044("Dokumentasjon av etterlønn/sluttvederlag"),
    I000045("Beskrivelse av funksjonsnedsettelse"),
    I000047("Brukeropplastet dokumentasjon"),
    I000049("Annet skjema (ikke NAV-skjema)"),
    I000051("Bekreftelse på deltakelse i kvalifiseringsprogrammet"),
    I000060("Annet"),
    I000061("Bekreftelse fra studiested/skole"),
    I000062("Bekreftelse på ventet fødselsdato"),
    I000063("Fødselsattest"),
    I000064("Elevdokumentasjon fra lærested"),
    I000065("Bekreftelse fra arbeidsgiver"),
    I000066("Kopi av likningsattest eller selvangivelse"),
    I000107("Vurdering av arbeidsmulighet/sykmelding"),
    I000108("Opplysninger om muligheter og behov for tilrettelegging ved svangerskap"),
    I000109("Skjema for tilrettelegging og omplassering ved graviditet"),
    I000110("Dokumentasjon av aleneomsorg"),
    I000111("Dokumentasjon av begrunnelse for hvorfor man søker tilbake i tid"),
    I000112("Dokumentasjon av deltakelse i introduksjonsprogrammet"),
    I000116("Bekreftelse på øvelse eller tjeneste i Forsvaret eller Sivilforsvaret"),
    I000117("Bekreftelse på tiltak i regi av Arbeids- og velferdsetaten"),
    I000118("Begrunnelse for sen søknad"),
    I000120("Dokumentasjon på at mor er innlagt på sykehus"),
    I000121("Dokumentasjon på at mor er syk"),
    I000122("Dokumentasjon på at far/medmor er innlagt på sykehus"),
    I000123("Dokumentasjon på at far/medmor er syk"),
    I000124("Dokumentasjon på at barnet er innlagt på sykehus"),
    I000130("Dokumentasjon på at mor studerer og arbeider til sammen heltid"),
    I000131("Dokumentasjon på at mor studerer på heltid"),
    I000132("Dokumentasjon på at mor er i arbeid"),
    I000133("Dokumentasjon av mors deltakelse i kvalifiseringsprogrammet"),
    I000140("Skattemelding"),
    I000141("Terminbekreftelse"),
    I000143("Dokumentasjon på oppholdstillatelse"),
    I000144("Dokumentasjon på reiser til og fra Norge"),
    I000145("Dokumentasjon på oppfølging i svangerskapet"),
    I000146("Dokumentasjon på inntekt"),

    // Identifisering av behandlingstema
    I500001("Ettersendelse til søknad om svangerskapspenger til selvstendig næringsdrivende og frilanser"),
    I500002("Ettersendelse til søknad om foreldrepenger ved adopsjon"),
    I500003("Ettersendelse til søknad om engangsstønad ved fødsel"),
    I500004("Ettersendelse til søknad om engangsstønad ved adopsjon"),
    I500005("Ettersendelse til søknad om foreldrepenger ved fødsel"),
    I500006("Ettersendelse til utsettelse eller gradert uttak av foreldrepenger (fleksibelt uttak)"),
    I500050("Ettersendelse til søknad om endring av uttak av foreldrepenger eller overføring av kvote"),
    ;

    private static final Set<DokumentTypeHistoriske> VEDLEGG_TYPER = Set.of(
            I000007,
            I000023,
            I000026,
            I000032,
            I000036,
            I000037,
            I000038,
            I000039,
            I000041,
            I000042,
            I000043,
            I000044,
            I000045,
            I000047,
            I000049,
            I000051,
            I000060,
            I000061,
            I000062,
            I000063,
            I000064,
            I000065,
            I000066,
            I000107,
            I000108,
            I000109,
            I000110,
            I000111,
            I000112,
            I000116,
            I000117,
            I000118,
            I000120,
            I000121,
            I000122,
            I000123,
            I000124,
            I000130,
            I000131,
            I000132,
            I000133,
            I000140,
            I000141,
            I500027
    );

    public static final Set<DokumentTypeHistoriske> FØRSTEGANGSSØKNAD_TYPER = Set.of(
            I000001,
            I000002,
            I000003,
            I000004,
            I000005,
            I000006
    );


    public static final Set<DokumentTypeHistoriske> ENDRINGSSØKNAD_TYPER = Set.of(I000050);


    private final String tittel;

    DokumentTypeHistoriske(String tittel) {
        this.tittel = tittel;
    }

    public String getTittel() {
        return tittel;
    }

    public static DokumentTypeHistoriske fraTittel(String tittel) {
        return Arrays.stream(values())
                .filter(dokumentTypeId -> dokumentTypeId.getTittel().equals(tittel))
                .findFirst()
                .orElseThrow();
    }

    public boolean erFørstegangssøknad() {
        return FØRSTEGANGSSØKNAD_TYPER.contains(this);
    }

    public boolean erEndringssøknad() {
        return ENDRINGSSØKNAD_TYPER.contains(this);
    }

    public boolean erVedlegg() {
        return VEDLEGG_TYPER.contains(this);
    }

    public boolean erKlage() {
        return I000027.equals(this);
    }

    public boolean erInntektsmelding() {
        return I000067.equals(this);
    }

    public boolean erUttalelseOmTilbakekreving() {
        return I000114.equals(this) || I000119.equals(this);
    }
}
