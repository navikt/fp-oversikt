package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import static no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Aktivitet;
import static no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Arbeidstidprosent;
import static no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.EøsUttakDto;
import static no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Gradering;
import static no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Rolle;
import static no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.SamtidigUttak;
import static no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakDto;
import static no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.VedtattResultat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.MorsAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.OppholdÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.OverføringÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.UtsettelseÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.VirkedagJusterer;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.soknadsplan.Søknadsplan;
import no.nav.foreldrepenger.oversikt.soknadsplan.SøknadsplanPeriode;

final class UttaksplanMapper {

    private UttaksplanMapper() {
    }

    static UttakDto mapUttak(Uttaksperiode periode, BrukerRolle brukerRolle) {
        var rolle = mapRolle(brukerRolle);
        var kontoType = periode.utledKontoType().map(Konto::tilDto).orElse(null);
        var utsettelseÅrsak = mapUtsettelseÅrsak(periode.utsettelseÅrsak());
        var overføringÅrsak = mapOverføringÅrsak(periode.overføringÅrsak());
        var morsAktivitet = periode.morsAktivitet() == null ? null : periode.morsAktivitet().tilDto();
        var samtidigUttak = periode.samtidigUttak() == null || !periode.samtidigUttak().merEnn0() ? null
            : new SamtidigUttak(periode.samtidigUttak().decimalValue());
        var gradering = utledGradering(periode).orElse(null);
        var resultat = mapResultat(periode.resultat());
        return new UttakDto(rolle, kontoType, utsettelseÅrsak, overføringÅrsak, gradering, morsAktivitet, samtidigUttak,
            periode.flerbarnsdager() != null && periode.flerbarnsdager(), resultat);
    }

    static UttakDto mapUttak(FpSøknadsperiode periode, BrukerRolle brukerRolle) {
        var samtidigUttak =
            periode.samtidigUttak() == null || !periode.samtidigUttak().merEnn0() ? null : new SamtidigUttak(periode.samtidigUttak().decimalValue());
        var gradering = periode.gradering() != null ? new Gradering(new Arbeidstidprosent(periode.gradering().prosent().decimalValue()),
            mapAktivitet(periode.gradering().uttakAktivitet())) : null;
        return new UttakDto(mapRolle(brukerRolle), periode.konto() == null ? null : periode.konto().tilDto(),
            mapUtsettelseÅrsak(periode.utsettelseÅrsak()), mapOverføringÅrsak(periode.overføringÅrsak()), gradering,
            periode.morsAktivitet() == null ? null : periode.morsAktivitet().tilDto(), samtidigUttak,
            periode.flerbarnsdager() != null && periode.flerbarnsdager(), null);
    }

    static List<Planperiode> mapVedtaksperioder(Collection<Uttaksperiode> perioder, BrukerRolle brukerRolle) {
        return Stream.ofNullable(perioder)
            .flatMap(Collection::stream)
            .filter(UttaksplanMapper::skalMedIPlan)
            .map(periode -> justerPlanperiode(periode.fom(), periode.tom(), mapUttak(periode, brukerRolle)))
            .flatMap(Optional::stream)
            .toList();
    }

    static List<Planperiode> mapSøknadsperioder(Collection<FpSøknadsperiode> perioder, BrukerRolle brukerRolle) {
        return Stream.ofNullable(perioder)
            .flatMap(Collection::stream)
            .filter(UttaksplanMapper::skalMedIPlan)
            .map(periode -> justerPlanperiode(periode.fom(), periode.tom(), mapUttak(periode, brukerRolle)))
            .flatMap(Optional::stream)
            .toList();
    }

    static List<Planperiode> mapLagredeSøknadsperioder(Collection<SøknadsplanPeriode> perioder) {
        return Stream.ofNullable(perioder)
            .flatMap(Collection::stream)
            .filter(UttaksplanMapper::skalMedIPlan)
            .map(periode -> justerPlanperiode(periode.fom(), periode.tom(), mapUttak(periode)))
            .flatMap(Optional::stream)
            .toList();
    }

    private static UttakDto mapUttak(SøknadsplanPeriode periode) {
        var gradering = periode.graderingArbeidstidprosent() == null ? null
            : new Gradering(new Arbeidstidprosent(periode.graderingArbeidstidprosent()), mapLagretAktivitet(periode));
        var samtidigUttak = periode.samtidigUttaksprosent() == null ? null : new SamtidigUttak(periode.samtidigUttaksprosent());
        return new UttakDto(mapRolle(periode.rolle()), mapKontoType(periode.kontoType()), mapUtsettelseÅrsak(periode.utsettelseÅrsak()),
            mapLagretOverføringÅrsak(periode.overføringÅrsak()), gradering, mapMorsAktivitet(periode.morsAktivitet()), samtidigUttak,
            Boolean.TRUE.equals(periode.flerbarnsdager()), null);
    }

    private static Aktivitet mapLagretAktivitet(SøknadsplanPeriode periode) {
        var aktivitetType = periode.aktivitetType();
        if (aktivitetType == null) {
            return null;
        }
        var arbeidsgiver = periode.arbeidsgiverId() == null ? null
            : mapArbeidsgiver(new Arbeidsgiver(periode.arbeidsgiverId()));
        return new Aktivitet(switch (aktivitetType) {
            case ORDINÆRT_ARBEID -> Aktivitet.AktivitetType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Aktivitet.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> Aktivitet.AktivitetType.FRILANS;
        }, arbeidsgiver, null);
    }

    private static no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType mapKontoType(SøknadsplanPeriode.KontoType kontoType) {
        if (kontoType == null) {
            return null;
        }
        return switch (kontoType) {
            case FORELDREPENGER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FORELDREPENGER;
            case MØDREKVOTE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.MØDREKVOTE;
            case FEDREKVOTE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FEDREKVOTE;
            case FELLESPERIODE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FELLESPERIODE;
            case FORELDREPENGER_FØR_FØDSEL -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FORELDREPENGER_FØR_FØDSEL;
        };
    }

    private static no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet mapMorsAktivitet(SøknadsplanPeriode.MorsAktivitet aktivitet) {
        if (aktivitet == null) {
            return null;
        }
        return switch (aktivitet) {
            case ARBEID -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.ARBEID;
            case UTDANNING -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.UTDANNING;
            case KVALPROG -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.KVALPROG;
            case INTROPROG -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.IKKE_OPPGITT;
        };
    }

    private static FellesUttaksplanDto.UtsettelseÅrsak mapUtsettelseÅrsak(SøknadsplanPeriode.UtsettelseÅrsak årsak) {
        if (årsak == null) {
            return null;
        }
        return switch (årsak) {
            case HV_ØVELSE -> FellesUttaksplanDto.UtsettelseÅrsak.HV_ØVELSE;
            case ARBEID -> FellesUttaksplanDto.UtsettelseÅrsak.ARBEID;
            case FERIE -> FellesUttaksplanDto.UtsettelseÅrsak.FERIE;
            case SØKER_SYKDOM -> FellesUttaksplanDto.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> FellesUttaksplanDto.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> FellesUttaksplanDto.UtsettelseÅrsak.BARN_INNLAGT;
            case NAV_TILTAK -> FellesUttaksplanDto.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> FellesUttaksplanDto.UtsettelseÅrsak.FRI;
        };
    }

    private static FellesUttaksplanDto.OverføringÅrsak mapLagretOverføringÅrsak(SøknadsplanPeriode.OverføringÅrsak årsak) {
        if (årsak == null) {
            return null;
        }
        return switch (årsak) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> FellesUttaksplanDto.OverføringÅrsak.ALENEOMSORG;
        };
    }

    private static Rolle mapRolle(Søknadsplan.Rolle rolle) {
        return switch (rolle) {
            case MOR -> Rolle.MOR;
            case FAR_MEDMOR -> Rolle.FAR_MEDMOR;
        };
    }

    private static boolean skalMedIPlan(SøknadsplanPeriode periode) {
        var utsettelse = periode.utsettelseÅrsak() == null ? null : switch (periode.utsettelseÅrsak()) {
            case HV_ØVELSE -> UtsettelseÅrsak.HV_ØVELSE;
            case ARBEID -> UtsettelseÅrsak.ARBEID;
            case FERIE -> UtsettelseÅrsak.LOVBESTEMT_FERIE;
            case SØKER_SYKDOM -> UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> UtsettelseÅrsak.BARN_INNLAGT;
            case NAV_TILTAK -> UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> UtsettelseÅrsak.FRI;
        };
        var morsAktivitet = periode.morsAktivitet() == null ? null : switch (periode.morsAktivitet()) {
            case ARBEID -> MorsAktivitet.ARBEID;
            case UTDANNING -> MorsAktivitet.UTDANNING;
            case KVALPROG -> MorsAktivitet.KVALPROG;
            case INTROPROG -> MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> MorsAktivitet.IKKE_OPPGITT;
        };
        return skalBeholdePeriodeUtFraOppholdOgUtsettelse(null, utsettelse, morsAktivitet);
    }

    private static boolean skalMedIPlan(Uttaksperiode periode) {
        return skalBeholdePeriodeUtFraOppholdOgUtsettelse(periode.oppholdÅrsak(), periode.utsettelseÅrsak(), periode.morsAktivitet())
            && skalBeholdePeriodeUtFraResultat(periode);
    }

    private static boolean skalMedIPlan(FpSøknadsperiode periode) {
        return skalBeholdePeriodeUtFraOppholdOgUtsettelse(periode.oppholdÅrsak(), periode.utsettelseÅrsak(), periode.morsAktivitet());
    }

    private static boolean skalBeholdePeriodeUtFraOppholdOgUtsettelse(OppholdÅrsak oppholdÅrsak,
                                                                      UtsettelseÅrsak utsettelseÅrsak,
                                                                      MorsAktivitet morsAktivitet) {
        if (utsettelseÅrsak != null) {
            return skalBeholdeUtsettelse(utsettelseÅrsak, morsAktivitet);
        }
        return oppholdÅrsak == null;
    }

    private static boolean skalBeholdeUtsettelse(UtsettelseÅrsak utsettelseÅrsak, MorsAktivitet morsAktivitet) {
        return utsettelseÅrsak != UtsettelseÅrsak.FRI || morsAktivitet != null;
    }

    private static Optional<Planperiode> justerPlanperiode(LocalDate fom, LocalDate tom, UttakDto uttak) {
        var justertFom = VirkedagJusterer.justerFom(fom);
        var justertTom = VirkedagJusterer.justerTom(tom);
        return justertFom.isAfter(justertTom) ? Optional.empty() : Optional.of(new Planperiode(justertFom, justertTom, uttak));
    }

    static EøsUttakDto mapEøsUttak(UttakPeriodeAnnenpartEøs periode) {
        return new EøsUttakDto(periode.kontoType().tilDto(),
            new EøsUttakDto.Trekkdager(periode.trekkdager()));
    }

    static FellesUttaksplanDto.Dekningsgrad mapDekningsgrad(Dekningsgrad dekningsgrad) {
        if (dekningsgrad == null) {
            return null;
        }
        return switch (dekningsgrad) {
            case ÅTTI -> FellesUttaksplanDto.Dekningsgrad.ÅTTI;
            case HUNDRE -> FellesUttaksplanDto.Dekningsgrad.HUNDRE;
        };
    }

    private static Optional<Gradering> utledGradering(Uttaksperiode periode) {
        if (periode.resultat() != null && Uttaksperiode.Resultat.Type.INNVILGET.equals(periode.resultat().type())) {
            return Optional.empty();
        }
        return aktiviteter(periode).max(Comparator.comparing(Uttaksperiode.UttaksperiodeAktivitet::arbeidstidsprosent))
            .filter(aktivitet -> aktivitet.arbeidstidsprosent().merEnn0())
            .map(aktivitet -> new Gradering(
                new Arbeidstidprosent(aktivitet.arbeidstidsprosent().decimalValue()), mapAktivitet(aktivitet.aktivitet())));
    }

    private static Aktivitet mapAktivitet(UttakAktivitet aktivitet) {
        if (aktivitet == null) {
            return null;
        }
        var arbeidsgiver = aktivitet.arbeidsgiver() == null ? null : mapArbeidsgiver(aktivitet.arbeidsgiver());
        return new Aktivitet(mapAktivitetType(aktivitet.type()), arbeidsgiver, null);
    }

    private static FellesUttaksplanDto.Arbeidsgiver mapArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        var dto = arbeidsgiver.tilDto();
        var type = switch (dto.type()) {
            case ORGANISASJON -> FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.ORGANISASJON;
            case PRIVAT -> FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.PRIVAT;
        };
        return new FellesUttaksplanDto.Arbeidsgiver(dto.id(), type);
    }

    private static Aktivitet.AktivitetType mapAktivitetType(UttakAktivitet.Type type) {
        return switch (type) {
            case ORDINÆRT_ARBEID -> Aktivitet.AktivitetType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Aktivitet.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> Aktivitet.AktivitetType.FRILANS;
            case ANNET -> Aktivitet.AktivitetType.ANNET;
        };
    }

    private static FellesUttaksplanDto.UtsettelseÅrsak mapUtsettelseÅrsak(UtsettelseÅrsak årsak) {
        if (årsak == null) {
            return null;
        }
        return switch (årsak) {
            case HV_ØVELSE -> FellesUttaksplanDto.UtsettelseÅrsak.HV_ØVELSE;
            case ARBEID -> FellesUttaksplanDto.UtsettelseÅrsak.ARBEID;
            case LOVBESTEMT_FERIE -> FellesUttaksplanDto.UtsettelseÅrsak.FERIE;
            case SØKER_SYKDOM -> FellesUttaksplanDto.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> FellesUttaksplanDto.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> FellesUttaksplanDto.UtsettelseÅrsak.BARN_INNLAGT;
            case NAV_TILTAK -> FellesUttaksplanDto.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> FellesUttaksplanDto.UtsettelseÅrsak.FRI;
        };
    }

    private static FellesUttaksplanDto.OverføringÅrsak mapOverføringÅrsak(OverføringÅrsak årsak) {
        if (årsak == null) {
            return null;
        }
        return switch (årsak) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> FellesUttaksplanDto.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> FellesUttaksplanDto.OverføringÅrsak.ALENEOMSORG;
        };
    }

    private static VedtattResultat mapResultat(Uttaksperiode.Resultat resultat) {
        if (resultat == null) {
            return null;
        }
        var trekkerDager = Stream.ofNullable(resultat.aktiviteter())
            .flatMap(Collection::stream)
            .anyMatch(aktivitet -> aktivitet.trekkdager().merEnn0());
        return new VedtattResultat(resultat.innvilget(), resultat.trekkerMinsterett(), trekkerDager,
            mapÅrsak(resultat.årsak()));
    }

    private static VedtattResultat.Årsak mapÅrsak(Uttaksperiode.Resultat.Årsak årsak) {
        if (årsak == null) {
            return VedtattResultat.Årsak.ANNET;
        }
        return switch (årsak) {
            case AVSLAG_FRATREKK_PLEIEPENGER -> VedtattResultat.Årsak.AVSLAG_FRATREKK_PLEIEPENGER;
            case INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID -> VedtattResultat.Årsak.INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID;
            case ANNET, AVSLAG_HULL_I_UTTAKSPLAN, AVSLAG_UTSETTELSE_TILBAKE_I_TID -> VedtattResultat.Årsak.ANNET;
        };
    }

    private static Rolle mapRolle(BrukerRolle brukerRolle) {
        return switch (brukerRolle) {
            case MOR -> Rolle.MOR;
            case FAR, MEDMOR -> Rolle.FAR_MEDMOR;
            case UKJENT -> throw new IllegalStateException("Ukjent brukerRolle burde ikke oppstå");
        };
    }

    private static Stream<Uttaksperiode.UttaksperiodeAktivitet> aktiviteter(Uttaksperiode periode) {
        return periode.resultat() == null ? Stream.empty() : Stream.ofNullable(periode.resultat().aktiviteter()).flatMap(Collection::stream);
    }

    private static boolean skalBeholdePeriodeUtFraResultat(Uttaksperiode periode) {
        if (periode.resultat() == null || periode.resultat().innvilget()) {
            return true;
        }
        var aktiviteter = aktiviteter(periode).toList();
        return aktiviteter.isEmpty() || aktiviteter.stream().anyMatch(aktivitet -> aktivitet.trekkdager().merEnn0());
    }
}
