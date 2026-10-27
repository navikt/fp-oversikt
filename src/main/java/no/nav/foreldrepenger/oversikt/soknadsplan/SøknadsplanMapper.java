package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Arbeidsgiver;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakDto;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakPeriodeDto;

final class SøknadsplanMapper {

    private SøknadsplanMapper() {
    }

    static List<SøknadsplanPeriode> tilPerioder(List<UttakPeriodeDto> perioder) {
        return perioder.stream().flatMap(SøknadsplanMapper::tilPerioder).toList();
    }

    static Stream<SøknadsplanPeriode> tilPerioder(UttakPeriodeDto periode) {
        return Stream.of(periode.søker(), periode.annenPart())
            .filter(Objects::nonNull)
            .map(uttak -> tilPeriode(periode.fom(), periode.tom(), uttak));
    }

    private static SøknadsplanPeriode tilPeriode(LocalDate fom, LocalDate tom, UttakDto uttak) {
        var gradering = uttak.gradering();
        var aktivitet = gradering == null ? null : gradering.aktivitet();
        var arbeidsgiver = aktivitet == null ? null : aktivitet.arbeidsgiver();
        return new SøknadsplanPeriode(fom, tom, rolle(uttak.forelder()),
            konto(uttak.kontoType()), utsettelse(uttak.utsettelseÅrsak()), overføring(uttak.overføringÅrsak()),
            morsAktivitet(uttak.morsAktivitet()),
            gradering == null || gradering.arbeidstidprosent() == null ? null : gradering.arbeidstidprosent().value(),
            uttak.samtidigUttak() == null ? null : uttak.samtidigUttak().value(), uttak.flerbarnsdager(),
            aktivitet == null ? null : aktivitetType(aktivitet.type()),
            arbeidsgiverId(arbeidsgiver));
    }

    private static Søknadsplan.Rolle rolle(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Rolle rolle) {
        return switch (rolle) {
            case MOR -> Søknadsplan.Rolle.MOR;
            case FAR_MEDMOR -> Søknadsplan.Rolle.FAR_MEDMOR;
        };
    }

    private static SøknadsplanPeriode.KontoType konto(no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType konto) {
        return switch (konto) {
            case null -> null;
            case FORELDREPENGER -> SøknadsplanPeriode.KontoType.FORELDREPENGER;
            case MØDREKVOTE -> SøknadsplanPeriode.KontoType.MØDREKVOTE;
            case FEDREKVOTE -> SøknadsplanPeriode.KontoType.FEDREKVOTE;
            case FELLESPERIODE -> SøknadsplanPeriode.KontoType.FELLESPERIODE;
            case FORELDREPENGER_FØR_FØDSEL -> SøknadsplanPeriode.KontoType.FORELDREPENGER_FØR_FØDSEL;
        };
    }

    private static SøknadsplanPeriode.UtsettelseÅrsak utsettelse(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UtsettelseÅrsak årsak) {
        return switch (årsak) {
            case null -> null;
            case ARBEID -> SøknadsplanPeriode.UtsettelseÅrsak.ARBEID;
            case FERIE -> SøknadsplanPeriode.UtsettelseÅrsak.FERIE;
            case SØKER_SYKDOM -> SøknadsplanPeriode.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> SøknadsplanPeriode.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> SøknadsplanPeriode.UtsettelseÅrsak.BARN_INNLAGT;
            case HV_ØVELSE -> SøknadsplanPeriode.UtsettelseÅrsak.HV_ØVELSE;
            case NAV_TILTAK -> SøknadsplanPeriode.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> SøknadsplanPeriode.UtsettelseÅrsak.FRI;
        };
    }

    private static SøknadsplanPeriode.OverføringÅrsak overføring(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.OverføringÅrsak årsak) {
        return switch (årsak) {
            case null -> null;
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> SøknadsplanPeriode.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> SøknadsplanPeriode.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case ALENEOMSORG -> SøknadsplanPeriode.OverføringÅrsak.ALENEOMSORG;
            case IKKE_RETT_ANNEN_FORELDER -> SøknadsplanPeriode.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
        };
    }

    private static SøknadsplanPeriode.MorsAktivitet morsAktivitet(no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet aktivitet) {
        return switch (aktivitet) {
            case null -> null;
            case ARBEID -> SøknadsplanPeriode.MorsAktivitet.ARBEID;
            case UTDANNING -> SøknadsplanPeriode.MorsAktivitet.UTDANNING;
            case KVALPROG -> SøknadsplanPeriode.MorsAktivitet.KVALPROG;
            case INTROPROG -> SøknadsplanPeriode.MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> SøknadsplanPeriode.MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> SøknadsplanPeriode.MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> SøknadsplanPeriode.MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> SøknadsplanPeriode.MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> SøknadsplanPeriode.MorsAktivitet.IKKE_OPPGITT;
        };
    }

    private static SøknadsplanPeriode.AktivitetType aktivitetType(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Aktivitet.AktivitetType type) {
        return switch (type) {
            case null -> null;
            case FRILANS -> SøknadsplanPeriode.AktivitetType.FRILANS;
            case ORDINÆRT_ARBEID -> SøknadsplanPeriode.AktivitetType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> SøknadsplanPeriode.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case ANNET -> SøknadsplanPeriode.AktivitetType.ANNET;
        };
    }

    private static String arbeidsgiverId(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        var id = arbeidsgiver.id();
        if (id == null || !id.matches("[0-9]{9}|[0-9]{11}")) {
            throw new BadRequestException("Arbeidsgiver-ID må bestå av 9 eller 11 sifre");
        }
        return id;
    }
}
