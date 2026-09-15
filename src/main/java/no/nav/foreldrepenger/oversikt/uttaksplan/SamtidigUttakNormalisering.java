package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.math.BigDecimal;

final class SamtidigUttakNormalisering {

    private SamtidigUttakNormalisering() {
    }

    // TODO: Verifiser at backend skal videreføre frontendregelen: Har bare én part samtidig uttak,
    //  får den andre 100 prosent minus egen arbeidstid, eller 100 prosent uten gradering.
    static NormalisertUttak normaliser(FellesUttaksplanDto.UttakDto søker, FellesUttaksplanDto.UttakDto annenPart) {
        if (søker == null || annenPart == null) {
            return new NormalisertUttak(søker, annenPart);
        }
        if (søker.samtidigUttak() == null && annenPart.samtidigUttak() != null) {
            søker = medSamtidigUttak(søker);
        } else if (annenPart.samtidigUttak() == null && søker.samtidigUttak() != null) {
            annenPart = medSamtidigUttak(annenPart);
        }
        return new NormalisertUttak(søker, annenPart);
    }

    private static FellesUttaksplanDto.UttakDto medSamtidigUttak(FellesUttaksplanDto.UttakDto uttak) {
        var arbeidstid = uttak.gradering() == null ? BigDecimal.ZERO : uttak.gradering().arbeidstidprosent().value();
        var samtidigUttak = new FellesUttaksplanDto.SamtidigUttak(BigDecimal.valueOf(100).subtract(arbeidstid));
        return new FellesUttaksplanDto.UttakDto(uttak.forelder(), uttak.kontoType(), uttak.utsettelseÅrsak(), uttak.overføringÅrsak(),
            uttak.gradering(), uttak.morsAktivitet(), samtidigUttak, uttak.flerbarnsdager(), uttak.resultat());
    }

    record NormalisertUttak(FellesUttaksplanDto.UttakDto søker, FellesUttaksplanDto.UttakDto annenPart) {
    }
}
