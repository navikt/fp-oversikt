package no.nav.foreldrepenger.oversikt.søknadsuttaksplan;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakDto;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakPeriodeDto;

final class SøknadsuttaksplanMapper {

    private SøknadsuttaksplanMapper() {
    }

    static List<SøknadsuttaksplanPeriode> tilPerioder(List<UttakPeriodeDto> perioder) {
        return perioder.stream().flatMap(SøknadsuttaksplanMapper::tilPerioder).toList();
    }

    static Stream<SøknadsuttaksplanPeriode> tilPerioder(UttakPeriodeDto periode) {
        return Stream.of(periode.søker(), periode.annenPart())
            .filter(Objects::nonNull)
            .map(uttak -> tilPeriode(periode.fom(), periode.tom(), uttak));
    }

    private static SøknadsuttaksplanPeriode tilPeriode(LocalDate fom, LocalDate tom, UttakDto uttak) {
        var gradering = uttak.gradering();
        var aktivitet = gradering == null ? null : gradering.aktivitet();
        var arbeidsgiver = aktivitet == null ? null : aktivitet.arbeidsgiver();
        return new SøknadsuttaksplanPeriode(fom, tom, rolle(uttak.forelder()),
            konto(uttak.kontoType()), utsettelse(uttak.utsettelseÅrsak()), overføring(uttak.overføringÅrsak()),
            morsAktivitet(uttak.morsAktivitet()),
            gradering == null || gradering.arbeidstidprosent() == null ? null : gradering.arbeidstidprosent().value(),
            uttak.samtidigUttak() == null ? null : uttak.samtidigUttak().value(), uttak.flerbarnsdager(),
            aktivitet == null ? null : aktivitetType(aktivitet.type()),
            arbeidsgiver == null ? null : arbeidsgiver.id(),
            arbeidsgiver == null ? null : arbeidsgiverType(arbeidsgiver.type()));
    }

    private static Søknadsuttaksplan.Rolle rolle(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Rolle rolle) {
        return switch (rolle) {
            case MOR -> Søknadsuttaksplan.Rolle.MOR;
            case FAR_MEDMOR -> Søknadsuttaksplan.Rolle.FAR_MEDMOR;
        };
    }

    private static SøknadsuttaksplanPeriode.KontoType konto(no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType konto) {
        return switch (konto) {
            case null -> null;
            case FORELDREPENGER -> SøknadsuttaksplanPeriode.KontoType.FORELDREPENGER;
            case MØDREKVOTE -> SøknadsuttaksplanPeriode.KontoType.MØDREKVOTE;
            case FEDREKVOTE -> SøknadsuttaksplanPeriode.KontoType.FEDREKVOTE;
            case FELLESPERIODE -> SøknadsuttaksplanPeriode.KontoType.FELLESPERIODE;
            case FORELDREPENGER_FØR_FØDSEL -> SøknadsuttaksplanPeriode.KontoType.FORELDREPENGER_FØR_FØDSEL;
        };
    }

    private static SøknadsuttaksplanPeriode.UtsettelseÅrsak utsettelse(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UtsettelseÅrsak årsak) {
        return switch (årsak) {
            case null -> null;
            case ARBEID -> SøknadsuttaksplanPeriode.UtsettelseÅrsak.ARBEID;
            case FERIE -> SøknadsuttaksplanPeriode.UtsettelseÅrsak.FERIE;
            case SØKER_SYKDOM -> SøknadsuttaksplanPeriode.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> SøknadsuttaksplanPeriode.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> SøknadsuttaksplanPeriode.UtsettelseÅrsak.BARN_INNLAGT;
            case HV_ØVELSE -> SøknadsuttaksplanPeriode.UtsettelseÅrsak.HV_ØVELSE;
            case NAV_TILTAK -> SøknadsuttaksplanPeriode.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> SøknadsuttaksplanPeriode.UtsettelseÅrsak.FRI;
        };
    }

    private static SøknadsuttaksplanPeriode.OverføringÅrsak overføring(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.OverføringÅrsak årsak) {
        return switch (årsak) {
            case null -> null;
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> SøknadsuttaksplanPeriode.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> SøknadsuttaksplanPeriode.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case ALENEOMSORG -> SøknadsuttaksplanPeriode.OverføringÅrsak.ALENEOMSORG;
            case IKKE_RETT_ANNEN_FORELDER -> SøknadsuttaksplanPeriode.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
        };
    }

    private static SøknadsuttaksplanPeriode.MorsAktivitet morsAktivitet(no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet aktivitet) {
        return switch (aktivitet) {
            case null -> null;
            case ARBEID -> SøknadsuttaksplanPeriode.MorsAktivitet.ARBEID;
            case UTDANNING -> SøknadsuttaksplanPeriode.MorsAktivitet.UTDANNING;
            case KVALPROG -> SøknadsuttaksplanPeriode.MorsAktivitet.KVALPROG;
            case INTROPROG -> SøknadsuttaksplanPeriode.MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> SøknadsuttaksplanPeriode.MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> SøknadsuttaksplanPeriode.MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> SøknadsuttaksplanPeriode.MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> SøknadsuttaksplanPeriode.MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> SøknadsuttaksplanPeriode.MorsAktivitet.IKKE_OPPGITT;
        };
    }

    private static SøknadsuttaksplanPeriode.AktivitetType aktivitetType(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Aktivitet.AktivitetType type) {
        return switch (type) {
            case null -> null;
            case FRILANS -> SøknadsuttaksplanPeriode.AktivitetType.FRILANS;
            case ORDINÆRT_ARBEID -> SøknadsuttaksplanPeriode.AktivitetType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> SøknadsuttaksplanPeriode.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case ANNET -> SøknadsuttaksplanPeriode.AktivitetType.ANNET;
        };
    }

    private static SøknadsuttaksplanPeriode.ArbeidsgiverType arbeidsgiverType(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType type) {
        return switch (type) {
            case null -> null;
            case PRIVAT -> SøknadsuttaksplanPeriode.ArbeidsgiverType.PRIVAT;
            case ORGANISASJON -> SøknadsuttaksplanPeriode.ArbeidsgiverType.ORGANISASJON;
        };
    }
}
