package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.math.BigDecimal;

import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

final class SamtidigUttakNormalisering {

    private SamtidigUttakNormalisering() {
    }

    static NormalisertUttak normaliser(FellesUttaksplanDto.UttakDto søker, FellesUttaksplanDto.UttakDto annenPart) {
        if (søker == null || annenPart == null) {
            return new NormalisertUttak(søker, annenPart);
        }
        if (søker.samtidigUttak() == null && annenPart.samtidigUttak() != null) {
            søker = medSamtidigUttak(søker);
        } else if (annenPart.samtidigUttak() == null && søker.samtidigUttak() != null) {
            annenPart = medSamtidigUttak(annenPart);
        }
        // Overlapp uten samtidig uttak hos noen av partene skjer ved berørt behandling. Søkers uttak vinner her (selv om den kanskje ikke vinner i berørt),
        // med mindre søkers periode er avslått uten å trekke dager og dermed ikke okkuperer tiden. Logikk hentes fra frontend
        // Utsettelser er allerede håndtert i UtsettelseOverlappNormalisering.
        if (søker.samtidigUttak() == null && annenPart.samtidigUttak() == null && okkupererTid(søker)) {
            annenPart = null;
        }
        return new NormalisertUttak(søker, annenPart);
    }

    private static boolean okkupererTid(FellesUttaksplanDto.UttakDto uttak) {
        var resultat = uttak.resultat();
        return resultat == null || resultat.innvilget() || resultat.trekkerDager();
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
