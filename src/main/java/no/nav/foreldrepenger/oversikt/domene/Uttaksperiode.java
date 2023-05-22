package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.common.innsyn.Aktivitet;
import no.nav.foreldrepenger.common.innsyn.Gradering;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.common.innsyn.UttakPeriodeResultat;

public record Uttaksperiode(LocalDate fom, LocalDate tom, Resultat resultat) {

    public UttakPeriode tilDto() {
        //TODO resten av resultat
        var trekkerDager = resultat().aktiviteter().stream().anyMatch(a -> a.trekkdager().merEnn0());
        var res = new UttakPeriodeResultat(resultat().innvilget(), false, trekkerDager, null);
        var gradertAktivitet = finnGradertAktivitet(resultat());

        //TODO sjekke gradering opp mot samtidig uttak som i fpinfo
        var gradering = gradertAktivitet.map(a -> new Gradering(new Gradering.Arbeidstidprosent(a.arbeidstidsprosent().decimalValue()), new Aktivitet(
            switch (a.aktivitet.type) {
                case ORDINÆRT_ARBEID -> Aktivitet.Type.ORDINÆRT_ARBEID;
                case SELVSTENDIG_NÆRINGSDRIVENDE -> Aktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
                case FRILANS -> Aktivitet.Type.FRILANS;
                case ANNET -> Aktivitet.Type.ANNET;
            }, a.aktivitet.arbeidsgiver == null ? null : a.aktivitet.arbeidsgiver.tilDto())));

        var konto = utledKontoType(resultat());
        return new UttakPeriode(fom, tom, konto.map(Konto::tilDto).orElse(null), res, null, null, null, gradering.orElse(null), null, null, false);
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

    public record Resultat(Type type, Set<UttaksperiodeAktivitet> aktiviteter) {

        public boolean innvilget() {
            return Objects.equals(type, Type.INNVILGET);
        }

        public boolean trekkerFraKonto(Konto konto) {
            return safeStream(aktiviteter()).anyMatch(a -> a.trekkdager().merEnn0() && konto.equals(a.konto()));
        }

        public enum Type {
            INNVILGET,
            AVSLÅTT
        }
    }

    public record UttaksperiodeAktivitet(UttakAktivitet aktivitet, Konto konto, Trekkdager trekkdager, Arbeidstidsprosent arbeidstidsprosent) {

    }

    public record UttakAktivitet(Type type, Arbeidsgiver arbeidsgiver, String arbeidsforholdId) {
        public enum Type {
            ORDINÆRT_ARBEID,
            SELVSTENDIG_NÆRINGSDRIVENDE,
            FRILANS,
            ANNET
        }
    }
}
