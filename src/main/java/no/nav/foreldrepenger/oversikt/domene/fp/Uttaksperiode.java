package no.nav.foreldrepenger.oversikt.domene.fp;


import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeResultat;
import no.nav.foreldrepenger.oversikt.domene.Prosent;

public record Uttaksperiode(LocalDate fom, LocalDate tom, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                            OverføringÅrsak overføringÅrsak, Prosent samtidigUttak, Boolean flerbarnsdager,
                            MorsAktivitet morsAktivitet, Resultat resultat) {

    public Optional<Konto> utledKontoType() {
        if (resultat() == null) {
            return Optional.empty();
        }
        return finnGradertAktivitet(resultat()).map(UttaksperiodeAktivitet::konto)
            .or(() -> Stream.ofNullable(resultat().aktiviteter())
                .flatMap(Collection::stream)
                .max(Comparator.comparing(UttaksperiodeAktivitet::trekkdager))
                .map(UttaksperiodeAktivitet::konto));
    }

    private Optional<UttaksperiodeAktivitet> finnGradertAktivitet(Resultat resultat) {
        if (Resultat.Type.INNVILGET.equals(resultat.type())) {
            return Optional.empty();
        }
        return Stream.ofNullable(resultat.aktiviteter())
            .flatMap(Collection::stream)
            .max(Comparator.comparing(UttaksperiodeAktivitet::arbeidstidsprosent))
            .filter(a -> a.arbeidstidsprosent.merEnn0());
    }

    public boolean harTrekkdager() {
        return resultat != null && resultat.harTrekkdager();
    }

    public record Resultat(Type type, Årsak årsak, Set<UttaksperiodeAktivitet> aktiviteter, boolean trekkerMinsterett) {

        public boolean innvilget() {
            return Set.of(Type.INNVILGET, Type.INNVILGET_GRADERING).contains(type());
        }

        public boolean trekkerFraKonto(Konto konto) {
            return Stream.ofNullable(aktiviteter())
                .flatMap(Collection::stream)
                .anyMatch(a -> a.trekkdager().merEnn0() && konto.equals(a.konto()));
        }

        public boolean harTrekkdager() {
            return Stream.ofNullable(aktiviteter).flatMap(Collection::stream).anyMatch(a -> a.trekkdager() != null && a.trekkdager().merEnn0());
        }

        public enum Type {
            INNVILGET,
            INNVILGET_GRADERING,
            AVSLÅTT
        }

        public enum Årsak {
            AVSLAG_HULL_I_UTTAKSPLAN,
            AVSLAG_FRATREKK_PLEIEPENGER,
            AVSLAG_UTSETTELSE_TILBAKE_I_TID,
            INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID,
            ANNET;

            public UttakPeriodeResultat.UttakPeriodeResultatÅrsak tilDto() {
                return switch (this) {
                    case AVSLAG_HULL_I_UTTAKSPLAN -> UttakPeriodeResultat.UttakPeriodeResultatÅrsak.AVSLAG_HULL_MELLOM_FORELDRENES_PERIODER;
                    case AVSLAG_FRATREKK_PLEIEPENGER -> UttakPeriodeResultat.UttakPeriodeResultatÅrsak.AVSLAG_FRATREKK_PLEIEPENGER;
                    case AVSLAG_UTSETTELSE_TILBAKE_I_TID -> UttakPeriodeResultat.UttakPeriodeResultatÅrsak.AVSLAG_UTSETTELSE_TILBAKE_I_TID;
                    case INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID -> UttakPeriodeResultat.UttakPeriodeResultatÅrsak.INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID;
                    case ANNET -> UttakPeriodeResultat.UttakPeriodeResultatÅrsak.ANNET;
                };
            }
        }
    }

    public record UttaksperiodeAktivitet(UttakAktivitet aktivitet, Konto konto, Trekkdager trekkdager, Prosent arbeidstidsprosent) {
    }
}
