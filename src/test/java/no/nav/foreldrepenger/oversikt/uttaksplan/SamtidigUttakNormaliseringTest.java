package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto;

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
    void skalIkkeSetteSamtidigUttakNårBeggeManglerVerdi() {
        var søker = uttak(FellesUttaksplanDto.Rolle.MOR, null, null);
        var annenPart = uttak(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, null);

        var normalisert = SamtidigUttakNormalisering.normaliser(søker, annenPart);

        assertThat(normalisert.søker()).isSameAs(søker);
        assertThat(normalisert.annenPart()).isSameAs(annenPart);
    }

    @Test
    void skalBeholdeManglendePart() {
        var søker = uttak(FellesUttaksplanDto.Rolle.MOR, null, 100);

        var normalisert = SamtidigUttakNormalisering.normaliser(søker, null);

        assertThat(normalisert.søker()).isSameAs(søker);
        assertThat(normalisert.annenPart()).isNull();
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
