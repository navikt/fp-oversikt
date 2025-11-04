package no.nav.foreldrepenger.oversikt.domene.fp;


import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.kontrakter.fpoversikt.Aktivitet;
import no.nav.foreldrepenger.kontrakter.fpoversikt.Arbeidstidprosent;
import no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak;
import no.nav.foreldrepenger.kontrakter.fpoversikt.Gradering;
import no.nav.foreldrepenger.kontrakter.fpoversikt.SamtidigUttak;
import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriode;
import no.nav.foreldrepenger.kontrakter.fpoversikt.UttakPeriodeResultat;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record Uttaksperiode(LocalDate fom, LocalDate tom, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                            OverføringÅrsak overføringÅrsak, Prosent samtidigUttak, Boolean flerbarnsdager,
                            MorsAktivitet morsAktivitet, Resultat resultat) {

    public UttakPeriode tilDto(BrukerRolleSak brukerRolle) {
        var trekkerDager = Stream.ofNullable(resultat().aktiviteter())
            .flatMap(Collection::stream)
            .anyMatch(a -> a.trekkdager().merEnn0());

        var utsettelse = utsettelseÅrsak() == null ? null : utsettelseÅrsak().tilDto();
        var opphold = oppholdÅrsak() == null ? null : oppholdÅrsak().tilDto();
        var overføring = overføringÅrsak() == null ? null : overføringÅrsak().tilDto();
        var ma = morsAktivitet() == null ? null : morsAktivitet().tilDto();

        var sa = samtidigUttak() == null || !samtidigUttak().merEnn0() ? null : new SamtidigUttak(samtidigUttak().decimalValue());

        //frontend vil ikke ha detaljer om gradering ved samtidigUttak
        var gradering = sa == null ? utledGradering().orElse(null) : null;

        var konto = utledKontoType(resultat());
        var res = new UttakPeriodeResultat(resultat().innvilget(), resultat().trekkerMinsterett(), trekkerDager, resultat().årsak().tilDto());
        return new UttakPeriode(fom, tom, konto.map(Konto::tilDto).orElse(null), res, utsettelse, opphold, overføring, gradering,
            ma, sa, flerbarnsdager() != null && flerbarnsdager(), brukerRolle);
    }

    public static List<UttakPeriode> compress(List<UttakPeriode> uttaksperioder) {
        var segments = uttaksperioder.stream().map(p -> new LocalDateSegment<>(p.fom(), p.tom(), p)).collect(Collectors.toSet());
        var timeline = new LocalDateTimeline<>(segments).compress(LocalDateInterval::abutsWorkdays, UttakPeriode::likBortsattFraTidsperiode,
            (datoInterval, datoSegment, datoSegment2) -> {
                var segmentValue = datoSegment.getValue();
                return new LocalDateSegment<>(datoInterval,
                    new UttakPeriode(datoInterval.getFomDato(), datoInterval.getTomDato(), segmentValue.kontoType(), segmentValue.resultat(),
                        segmentValue.utsettelseÅrsak(), segmentValue.oppholdÅrsak(), segmentValue.overføringÅrsak(), segmentValue.gradering(),
                        segmentValue.morsAktivitet(), segmentValue.samtidigUttak(), segmentValue.flerbarnsdager(), segmentValue.forelder()));
            });
        return timeline.stream().map(LocalDateSegment::getValue).toList();
    }

    private Optional<Gradering> utledGradering() {
        if (Resultat.Type.INNVILGET.equals(resultat.type())) {
            return Optional.empty();
        }
        var gradertAktivitet = finnGradertAktivitet(resultat());
        return gradertAktivitet.map(a -> new Gradering(new Arbeidstidprosent(a.arbeidstidsprosent().decimalValue()),
            new Aktivitet(a.aktivitet().type().tilDto(), a.aktivitet().arbeidsgiver() == null ? null : a.aktivitet().arbeidsgiver().tilDto(), null)));
    }

    private Optional<Konto> utledKontoType(Resultat resultat) {
        var gradertAktivitet = finnGradertAktivitet(resultat);
        if (gradertAktivitet.isPresent()) {
            return Optional.of(gradertAktivitet.get().konto());
        }
        return Stream.ofNullable(resultat().aktiviteter())
            .flatMap(Collection::stream)
            .max(Comparator.comparing(UttaksperiodeAktivitet::trekkdager))
            .map(UttaksperiodeAktivitet::konto);
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

    public record Resultat(Type type, Årsak årsak, Set<UttaksperiodeAktivitet> aktiviteter, boolean trekkerMinsterett) {

        public boolean innvilget() {
            return Set.of(Type.INNVILGET, Type.INNVILGET_GRADERING).contains(type());
        }

        public boolean trekkerFraKonto(Konto konto) {
            return Stream.ofNullable(aktiviteter())
                .flatMap(Collection::stream)
                .anyMatch(a -> a.trekkdager().merEnn0() && konto.equals(a.konto()));
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
