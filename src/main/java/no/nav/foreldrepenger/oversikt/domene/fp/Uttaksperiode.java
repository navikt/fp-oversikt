package no.nav.foreldrepenger.oversikt.domene.fp;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.common.innsyn.Aktivitet;
import no.nav.foreldrepenger.common.innsyn.Arbeidstidprosent;
import no.nav.foreldrepenger.common.innsyn.Gradering;
import no.nav.foreldrepenger.common.innsyn.SamtidigUttak;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.common.innsyn.UttakPeriodeResultat;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.domene.Prosent;

public record Uttaksperiode(LocalDate fom, LocalDate tom, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                            OverføringÅrsak overføringÅrsak, Prosent samtidigUttak, Boolean flerbarnsdager,
                            MorsAktivitet morsAktivitet, Resultat resultat) {

    private static final Environment ENV = Environment.current();

    public UttakPeriode tilDto() {
        var trekkerDager = safeStream(resultat().aktiviteter()).anyMatch(a -> a.trekkdager().merEnn0());

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
            ma, sa, flerbarnsdager() != null && flerbarnsdager());
    }

    private Optional<Gradering> utledGradering() {
        if (Resultat.Type.INNVILGET.equals(resultat.type())) {
            return Optional.empty();
        }
        var gradertAktivitet = finnGradertAktivitet(resultat());
        return gradertAktivitet.map(a -> new Gradering(new Arbeidstidprosent(a.arbeidstidsprosent().decimalValue()),
            new Aktivitet(a.aktivitet().type().tilDto(), a.aktivitet().arbeidsgiver() == null ? null : a.aktivitet().arbeidsgiver().tilDto())));
    }

    private Optional<Konto> utledKontoType(Resultat resultat) {
        var gradertAktivitet = finnGradertAktivitet(resultat);
        if (gradertAktivitet.isPresent()) {
            return Optional.of(gradertAktivitet.get().konto());
        }
        return safeStream(resultat().aktiviteter())
            .max(Comparator.comparing(UttaksperiodeAktivitet::trekkdager))
            .map(UttaksperiodeAktivitet::konto);
    }

    private Optional<UttaksperiodeAktivitet> finnGradertAktivitet(Resultat resultat) {
        return safeStream(resultat.aktiviteter())
            .max(Comparator.comparing(UttaksperiodeAktivitet::arbeidstidsprosent))
            .filter(a -> a.arbeidstidsprosent.merEnn0());
    }

    public record Resultat(Type type, Årsak årsak, Set<UttaksperiodeAktivitet> aktiviteter, boolean trekkerMinsterett) {

        public boolean innvilget() {
            return Set.of(Type.INNVILGET, Type.INNVILGET_GRADERING).contains(type());
        }

        public boolean trekkerFraKonto(Konto konto) {
            return safeStream(aktiviteter()).anyMatch(a -> a.trekkdager().merEnn0() && konto.equals(a.konto()));
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

            public UttakPeriodeResultat.Årsak tilDto() {
                if (!ENV.isProd()) {
                    return switch (this) {
                        case AVSLAG_HULL_I_UTTAKSPLAN -> UttakPeriodeResultat.Årsak.AVSLAG_HULL_MELLOM_FORELDRENES_PERIODER;
                        case AVSLAG_FRATREKK_PLEIEPENGER -> UttakPeriodeResultat.Årsak.AVSLAG_FRATREKK_PLEIEPENGER;
                        case AVSLAG_UTSETTELSE_TILBAKE_I_TID -> UttakPeriodeResultat.Årsak.AVSLAG_UTSETTELSE_TILBAKE_I_TID;
                        case INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID -> UttakPeriodeResultat.Årsak.INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID;
                        case ANNET -> UttakPeriodeResultat.Årsak.ANNET;
                    };
                } else {
                    return switch (this) {
                        case AVSLAG_HULL_I_UTTAKSPLAN -> UttakPeriodeResultat.Årsak.AVSLAG_HULL_MELLOM_FORELDRENES_PERIODER;
                        case AVSLAG_FRATREKK_PLEIEPENGER -> UttakPeriodeResultat.Årsak.AVSLAG_FRATREKK_PLEIEPENGER;
                        case AVSLAG_UTSETTELSE_TILBAKE_I_TID, INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID, ANNET -> UttakPeriodeResultat.Årsak.ANNET;
                    };
                }
            }
        }
    }

    public record UttaksperiodeAktivitet(UttakAktivitet aktivitet, Konto konto, Trekkdager trekkdager, Prosent arbeidstidsprosent) {

    }
}
