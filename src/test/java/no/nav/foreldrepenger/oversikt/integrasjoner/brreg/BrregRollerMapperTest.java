package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class BrregRollerMapperTest {

    @Test
    void skalMappeAktivInnehaverMedEnhetsdetaljer() {
        var rolleutskrift = enhet("123456789", "Navn fra rolleutskrift",
            List.of(rolle(false, false, "INNH")));
        var enhetsdetaljer = new BrregEnhetDto(
            "123456789",
            "Mitt gårdsbruk",
            new BrregEnhetDto.EnhetKodeDto("ENK", "Enkeltpersonforetak"),
            new BrregEnhetDto.EnhetKodeDto("01.110", "Dyrking av korn"),
            null,
            null,
            false,
            null,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 2, 1),
            null
        );

        var resultat = BrregRollerMapper.mapSelvstendigNæring(rolleutskrift, enhetsdetaljer);

        assertThat(resultat.navn()).isEqualTo("Mitt gårdsbruk");
        assertThat(resultat.organisasjonsformKode()).isEqualTo("ENK");
        assertThat(resultat.organisasjonsformBeskrivelse()).isEqualTo("Enkeltpersonforetak");
        assertThat(resultat.næringstype()).isEqualTo(Virksomhetstype.JORDBRUK_SKOGBRUK);
        assertThat(resultat.roller()).containsExactly(SNRolleType.INNEHAVER);
    }

    @Test
    void skalBrukeNavnFraRolleutskriftNårEnhetsdetaljerMangler() {
        var rolleutskrift = enhet("123456789", "Navn fra rolleutskrift",
            List.of(rolle(false, false, "INNH")));

        var resultat = BrregRollerMapper.mapSelvstendigNæring(rolleutskrift, null);

        assertThat(resultat.navn()).isEqualTo("Navn fra rolleutskrift");
        assertThat(resultat.næringstype()).isEqualTo(Virksomhetstype.ANNEN);
    }

    @Test
    void skalIgnorereFratraadteOgAvregistrerteRoller() {
        var rolleutskrift = enhet("123456789", "Virksomhet", List.of(
            rolle(true, false, "INNH"),
            rolle(false, true, "DTPR")
        ));

        assertThat(BrregRollerMapper.erSelvstendigNæringsdrivende(rolleutskrift)).isFalse();
    }

    @Test
    void skalMappeNæringskoderTilSøknadstyper() {
        assertThat(mapVirksomhetstype("03.110")).isEqualTo(Virksomhetstype.FISKE);
        assertThat(mapVirksomhetstype("88.911")).isEqualTo(Virksomhetstype.DAGMAMMA);
        assertThat(mapVirksomhetstype("02.100")).isEqualTo(Virksomhetstype.JORDBRUK_SKOGBRUK);
        assertThat(mapVirksomhetstype("01.610")).isEqualTo(Virksomhetstype.ANNEN);
        assertThat(mapVirksomhetstype("62.010")).isEqualTo(Virksomhetstype.ANNEN);
    }

    private static Virksomhetstype mapVirksomhetstype(String næringskode) {
        var rolleutskrift = enhet("123456789", "Virksomhet", List.of(rolle(false, false, "INNH")));
        var enhetsdetaljer = new BrregEnhetDto(
            "123456789",
            "Virksomhet",
            null,
            new BrregEnhetDto.EnhetKodeDto(næringskode, "Beskrivelse"),
            null,
            null,
            false,
            null,
            null,
            null,
            null
        );
        return BrregRollerMapper.mapSelvstendigNæring(rolleutskrift, enhetsdetaljer).næringstype();
    }

    private static BrregRolleutskriftDto.EnhetDto enhet(String organisasjonsnummer,
                                                        String navn,
                                                        List<BrregRolleutskriftDto.RolleDto> roller) {
        return new BrregRolleutskriftDto.EnhetDto(organisasjonsnummer, navn, roller, null);
    }

    private static BrregRolleutskriftDto.RolleDto rolle(boolean fratrådt, boolean avregistrert, String kode) {
        return new BrregRolleutskriftDto.RolleDto(
            fratrådt,
            avregistrert,
            new BrregRolleutskriftDto.RolleKodeDto(kode, kode)
        );
    }
}
