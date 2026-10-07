package no.nav.foreldrepenger.oversikt.uttaksplan;

import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.EøsUttakDto;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakDto;

final class UtsettelseOverlappNormalisering {

    private UtsettelseOverlappNormalisering() {
    }

    static NormalisertUttak normaliser(UttakDto søker, UttakDto annenPart, EøsUttakDto annenPartEøs) {
        // Frontend støtter per i dag ikke overlapp mellom uttak og utsettelse.
        // Tidslinjen er allerede delt, så vi fjerner bare utsettelsen i det overlappende delintervallet.
        if (søker == null || (annenPart == null && annenPartEøs == null)) {
            return new NormalisertUttak(søker, annenPart, annenPartEøs);
        }
        var søkerHarUttak = erUttak(søker);
        // EØS-perioder er alltid uttak og prioriteres derfor over søkers utsettelse.
        var annenPartHarUttak = annenPartEøs != null || erUttak(annenPart);
        if (søkerHarUttak) {
            if (annenPartHarUttak) {
                return new NormalisertUttak(søker, annenPart, annenPartEøs);
            }
            return new NormalisertUttak(søker, null, null);
        }
        if (erUtsettelse(søker) && erUtsettelse(annenPart)) {
            // Ved to utsettelser beholdes søkerens, siden planen vises fra søkerens perspektiv.
            return new NormalisertUttak(søker, null, null);
        }
        return new NormalisertUttak(null, annenPart, annenPartEøs);
    }

    private static boolean erUttak(UttakDto uttak) {
        return uttak != null && !erUtsettelse(uttak);
    }

    private static boolean erUtsettelse(UttakDto uttak) {
        return uttak != null && uttak.utsettelseÅrsak() != null;
    }

    record NormalisertUttak(UttakDto søker, UttakDto annenPart, EøsUttakDto annenPartEøs) {
    }
}
