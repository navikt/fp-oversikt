package no.nav.foreldrepenger.oversikt.søknad;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakDto;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakPeriodeDto;

final class SøknadMapper {

    private SøknadMapper() {
    }

    static List<SøknadPeriode> tilPerioder(List<UttakPeriodeDto> perioder) {
        return perioder.stream().flatMap(SøknadMapper::tilPerioder).toList();
    }

    static Stream<SøknadPeriode> tilPerioder(UttakPeriodeDto periode) {
        return Stream.of(periode.søker(), periode.annenPart())
            .filter(Objects::nonNull)
            .map(uttak -> tilPeriode(periode.fom(), periode.tom(), uttak));
    }

    private static SøknadPeriode tilPeriode(LocalDate fom, LocalDate tom, UttakDto uttak) {
        var gradering = uttak.gradering();
        var aktivitet = gradering == null ? null : gradering.aktivitet();
        var arbeidsgiver = aktivitet == null ? null : aktivitet.arbeidsgiver();
        return new SøknadPeriode(fom, tom, rolle(uttak.forelder()),
            konto(uttak.kontoType()), utsettelse(uttak.utsettelseÅrsak()), overføring(uttak.overføringÅrsak()),
            morsAktivitet(uttak.morsAktivitet()),
            gradering == null || gradering.arbeidstidprosent() == null ? null : gradering.arbeidstidprosent().value(),
            uttak.samtidigUttak() == null ? null : uttak.samtidigUttak().value(), uttak.flerbarnsdager(),
            aktivitet == null ? null : aktivitetType(aktivitet.type()),
            arbeidsgiver == null ? null : arbeidsgiver.id(),
            arbeidsgiver == null ? null : arbeidsgiverType(arbeidsgiver.type()));
    }

    private static Søknad.Rolle rolle(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Rolle rolle) {
        return switch (rolle) {
            case MOR -> Søknad.Rolle.MOR;
            case FAR_MEDMOR -> Søknad.Rolle.FAR_MEDMOR;
        };
    }

    private static SøknadPeriode.KontoType konto(no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType konto) {
        return switch (konto) {
            case null -> null;
            case FORELDREPENGER -> SøknadPeriode.KontoType.FORELDREPENGER;
            case MØDREKVOTE -> SøknadPeriode.KontoType.MØDREKVOTE;
            case FEDREKVOTE -> SøknadPeriode.KontoType.FEDREKVOTE;
            case FELLESPERIODE -> SøknadPeriode.KontoType.FELLESPERIODE;
            case FORELDREPENGER_FØR_FØDSEL -> SøknadPeriode.KontoType.FORELDREPENGER_FØR_FØDSEL;
        };
    }

    private static SøknadPeriode.UtsettelseÅrsak utsettelse(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UtsettelseÅrsak årsak) {
        return switch (årsak) {
            case null -> null;
            case ARBEID -> SøknadPeriode.UtsettelseÅrsak.ARBEID;
            case FERIE -> SøknadPeriode.UtsettelseÅrsak.FERIE;
            case SØKER_SYKDOM -> SøknadPeriode.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> SøknadPeriode.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> SøknadPeriode.UtsettelseÅrsak.BARN_INNLAGT;
            case HV_ØVELSE -> SøknadPeriode.UtsettelseÅrsak.HV_ØVELSE;
            case NAV_TILTAK -> SøknadPeriode.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> SøknadPeriode.UtsettelseÅrsak.FRI;
        };
    }

    private static SøknadPeriode.OverføringÅrsak overføring(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.OverføringÅrsak årsak) {
        return switch (årsak) {
            case null -> null;
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> SøknadPeriode.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> SøknadPeriode.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case ALENEOMSORG -> SøknadPeriode.OverføringÅrsak.ALENEOMSORG;
            case IKKE_RETT_ANNEN_FORELDER -> SøknadPeriode.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
        };
    }

    private static SøknadPeriode.MorsAktivitet morsAktivitet(no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet aktivitet) {
        return switch (aktivitet) {
            case null -> null;
            case ARBEID -> SøknadPeriode.MorsAktivitet.ARBEID;
            case UTDANNING -> SøknadPeriode.MorsAktivitet.UTDANNING;
            case KVALPROG -> SøknadPeriode.MorsAktivitet.KVALPROG;
            case INTROPROG -> SøknadPeriode.MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> SøknadPeriode.MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> SøknadPeriode.MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> SøknadPeriode.MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> SøknadPeriode.MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> SøknadPeriode.MorsAktivitet.IKKE_OPPGITT;
        };
    }

    private static SøknadPeriode.AktivitetType aktivitetType(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Aktivitet.AktivitetType type) {
        return switch (type) {
            case null -> null;
            case FRILANS -> SøknadPeriode.AktivitetType.FRILANS;
            case ORDINÆRT_ARBEID -> SøknadPeriode.AktivitetType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> SøknadPeriode.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case ANNET -> SøknadPeriode.AktivitetType.ANNET;
        };
    }

    private static SøknadPeriode.ArbeidsgiverType arbeidsgiverType(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType type) {
        return switch (type) {
            case null -> null;
            case PRIVAT -> SøknadPeriode.ArbeidsgiverType.PRIVAT;
            case ORGANISASJON -> SøknadPeriode.ArbeidsgiverType.ORGANISASJON;
        };
    }
}
