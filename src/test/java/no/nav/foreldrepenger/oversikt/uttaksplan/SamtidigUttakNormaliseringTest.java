package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

class SamtidigUttakNormaliseringTest {

    @Test
    void skalSetteHundreProsentNårUgradertSøkerManglerSamtidigUttak() {
        var søker = uttak(FellesUttaksplanDto.Rolle.MOR, null, null);
        var annenPart = uttak(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, 100);

        var normalisert = SamtidigUttakNormalisering.normaliser(søker, annenPart);

        assertThat(normalisert.søker().samtidigUttak().value()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(normalisert.annenPart()).isSameAs(annenPart);
    }

    @Test
    void skalSetteHundreProsentNårUgradertAnnenPartManglerSamtidigUttak() {
        var søker = uttak(FellesUttaksplanDto.Rolle.MOR, null, 100);
        var annenPart = uttak(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, null);

        var normalisert = SamtidigUttakNormalisering.normaliser(søker, annenPart);

        assertThat(normalisert.søker()).isSameAs(søker);
        assertThat(normalisert.annenPart().samtidigUttak().value()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skalSetteSamtidigUttakTilHundreMinusEgenArbeidstid() {
        var søker = uttak(FellesUttaksplanDto.Rolle.MOR, null, 60);
        var annenPart = uttak(FellesUttaksplanDto.Rolle.FAR_MEDMOR, 60, null);

        var normalisert = SamtidigUttakNormalisering.normaliser(søker, annenPart);

        assertThat(normalisert.annenPart().samtidigUttak().value()).isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    @Test
    void skalBeholdeEksisterendeVerdier() {
        var søker = uttak(FellesUttaksplanDto.Rolle.MOR, null, 60);
        var annenPart = uttak(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, 30);

        var normalisert = SamtidigUttakNormalisering.normaliser(søker, annenPart);

        assertThat(normalisert.søker()).isSameAs(søker);
        assertThat(normalisert.annenPart()).isSameAs(annenPart);
    }

    @Test
    void skalFjerneAnnenPartNårIngenAvParteneHarSamtidigUttak() {
        var søker = uttak(FellesUttaksplanDto.Rolle.MOR, null, null);
        var annenPart = uttak(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, null);

        var normalisert = SamtidigUttakNormalisering.normaliser(søker, annenPart);

        assertThat(normalisert.søker()).isSameAs(søker);
        assertThat(normalisert.annenPart()).isNull();
    }

    // Søkers egne perioder endres ikke, selv med 100 % samtidig uttak uten annen part. Søker kan ha samtidig uttak med en
    // annen part vi ikke ser (adressebeskyttelse, saker som ikke er koblet, annen part som ikke har søkt). Søknaden sender
    // dessuten hele planen i endringssøknaden, så en fjernet markering ville blitt sendt til fpsak for perioder søker ikke
    // har endret, for eksempel fars to uker rundt fødsel.
    @Test
    void skalBeholdeManglendePart() {
        var søker = uttak(FellesUttaksplanDto.Rolle.MOR, null, 100);

        var normalisert = SamtidigUttakNormalisering.normaliser(søker, null);

        assertThat(normalisert.søker()).isSameAs(søker);
        assertThat(normalisert.annenPart()).isNull();
    }

    // Annen part har 100 % samtidig uttak i en periode der søker ikke har uttak, f.eks. mor har søkt to uker og far én.
    // Da er det ikke samtidig med noe. Uten normaliseringen vises perioden som «begge» i søknaden, og endrer søker
    // datoene, forsvinner annen parts dager fra planen. Søker ser alltid sin egen sak, så når annen part vises, kjenner vi
    // alle søkers perioder. Søknaden gjør det samme når søker sletter sin del av en samtidig periode
    // (fjernSamtidigUttak i UttakPeriodeBuilder).
    @Test
    void skalFjerneHundreProsentSamtidigUttakNårBareAnnenPartHarUttak() {
        var annenPart = uttak(FellesUttaksplanDto.Rolle.MOR, null, 100);

        var normalisert = SamtidigUttakNormalisering.normaliser(null, annenPart);

        assertThat(normalisert.søker()).isNull();
        assertThat(normalisert.annenPart().samtidigUttak()).isNull();
        assertThat(normalisert.annenPart().kontoType()).isEqualTo(annenPart.kontoType());
        assertThat(normalisert.annenPart().forelder()).isEqualTo(annenPart.forelder());
    }

    // Samtidig uttak under 100 % hos annen part må beholdes, fordi prosenten påvirker hvor mange dager som trekkes.
    @Test
    void skalBeholdeGradertSamtidigUttakNårBareAnnenPartHarUttak() {
        var annenPart = uttak(FellesUttaksplanDto.Rolle.MOR, 50, 50);

        var normalisert = SamtidigUttakNormalisering.normaliser(null, annenPart);

        assertThat(normalisert.annenPart()).isSameAs(annenPart);
    }

    private static FellesUttaksplanDto.UttakDto uttak(FellesUttaksplanDto.Rolle rolle,
                                                       Integer arbeidstidsprosent,
                                                       Integer samtidigUttak) {
        var gradering = arbeidstidsprosent == null ? null : new FellesUttaksplanDto.Gradering(
            new FellesUttaksplanDto.Arbeidstidprosent(BigDecimal.valueOf(arbeidstidsprosent)), null);
        var samtidig = samtidigUttak == null ? null : new FellesUttaksplanDto.SamtidigUttak(BigDecimal.valueOf(samtidigUttak));
        return new FellesUttaksplanDto.UttakDto(rolle, KontoType.FELLESPERIODE, null, null, gradering, null, samtidig, false, null);
    }
}
