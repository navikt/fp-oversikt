package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.søknadsuttaksplan.Søknadsuttaksplan;
import no.nav.foreldrepenger.oversikt.søknadsuttaksplan.SøknadsuttaksplanPeriode;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

class UttaksplanMapperLagretSøknadsuttaksplanTest {
    private static final LocalDate MANDAG = LocalDate.of(2026, 10, 5);

    @Test
    void mapperRolleFelterAktivitetOgArbeidsgiver() {
        var mor = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG, SøknadsuttaksplanPeriode.KontoType.FELLESPERIODE,
            null, null, null, new BigDecimal("26.75"), null, false, SøknadsuttaksplanPeriode.AktivitetType.ORDINÆRT_ARBEID,
            "org-123", SøknadsuttaksplanPeriode.ArbeidsgiverType.ORGANISASJON);
        var far = periode(Søknadsuttaksplan.Rolle.FAR_MEDMOR, MANDAG.plusDays(1), MANDAG.plusDays(1),
            SøknadsuttaksplanPeriode.KontoType.FEDREKVOTE, null, SøknadsuttaksplanPeriode.OverføringÅrsak.ALENEOMSORG,
            SøknadsuttaksplanPeriode.MorsAktivitet.UTDANNING, new BigDecimal("50"), null, true, SøknadsuttaksplanPeriode.AktivitetType.FRILANS,
            "person-1", SøknadsuttaksplanPeriode.ArbeidsgiverType.PRIVAT);

        var resultater = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(mor, far));

        assertThat(resultater).hasSize(2);
        assertThat(resultater.get(0).uttak().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.MOR);
        assertThat(resultater.get(0).uttak().kontoType()).isEqualTo(no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FELLESPERIODE);
        assertThat(resultater.get(0).uttak().gradering().aktivitet().type()).isEqualTo(FellesUttaksplanDto.Aktivitet.AktivitetType.ORDINÆRT_ARBEID);
        assertThat(resultater.get(0).uttak().gradering().aktivitet().arbeidsgiver().id()).isEqualTo("org-123");
        assertThat(resultater.get(0).uttak().gradering().aktivitet().arbeidsgiverNavn()).isNull();
        assertThat(resultater.get(1).uttak().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.FAR_MEDMOR);
        assertThat(resultater.get(1).uttak().overføringÅrsak()).isEqualTo(FellesUttaksplanDto.OverføringÅrsak.ALENEOMSORG);
        assertThat(resultater.get(1).uttak().morsAktivitet()).isEqualTo(no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.UTDANNING);
        assertThat(resultater.get(1).uttak().flerbarnsdager()).isTrue();
        assertThat(resultater.get(1).uttak().gradering().aktivitet().arbeidsgiver().type())
            .isEqualTo(FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.PRIVAT);
    }

    @Test
    void beholderSamtidigUttakOgGraderingMedEksaktProsent() {
        var periode = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG,
            SøknadsuttaksplanPeriode.KontoType.MØDREKVOTE, SøknadsuttaksplanPeriode.UtsettelseÅrsak.ARBEID, null, null,
            new BigDecimal("12.3456789"), new BigDecimal("87.6543219"), null,
            SøknadsuttaksplanPeriode.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null);

        var uttak = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(periode)).getFirst().uttak();

        assertThat(uttak.utsettelseÅrsak()).isEqualTo(FellesUttaksplanDto.UtsettelseÅrsak.ARBEID);
        assertThat(uttak.gradering().arbeidstidprosent().value()).isEqualByComparingTo("12.3456789");
        assertThat(uttak.gradering().aktivitet().type()).isEqualTo(FellesUttaksplanDto.Aktivitet.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(uttak.samtidigUttak().value()).isEqualByComparingTo("87.6543219");
    }

    @Test
    void nullableFelterForblirNull() {
        var periode = periode(Søknadsuttaksplan.Rolle.FAR_MEDMOR, MANDAG, MANDAG,
            null, null, null, null, null, null, null, null, null, null);

        var uttak = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(periode)).getFirst().uttak();

        assertThat(uttak.kontoType()).isNull();
        assertThat(uttak.utsettelseÅrsak()).isNull();
        assertThat(uttak.overføringÅrsak()).isNull();
        assertThat(uttak.morsAktivitet()).isNull();
        assertThat(uttak.gradering()).isNull();
        assertThat(uttak.samtidigUttak()).isNull();
        assertThat(uttak.flerbarnsdager()).isFalse();
    }

    @Test
    void utelaterFriUtenMorsAktivitetOgBeholderFriMedAktivitet() {
        var friUten = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG, null,
            SøknadsuttaksplanPeriode.UtsettelseÅrsak.FRI, null, null, null, null, null, null, null, null);
        var friMed = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG.plusDays(1), MANDAG.plusDays(1), null,
            SøknadsuttaksplanPeriode.UtsettelseÅrsak.FRI, null, SøknadsuttaksplanPeriode.MorsAktivitet.ARBEID, null, null,
            null, null, null, null);

        var resultater = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(friUten, friMed));

        assertThat(resultater).hasSize(1);
        assertThat(resultater.getFirst().uttak().utsettelseÅrsak()).isEqualTo(FellesUttaksplanDto.UtsettelseÅrsak.FRI);
    }

    @Test
    void justererDatoerTilVirkedagerOgUtelaterTomtIntervall() {
        var arbeidsuke = periode(Søknadsuttaksplan.Rolle.MOR, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12),
            null, null, null, null, null, null, null, null, null, null);
        var tomt = periode(Søknadsuttaksplan.Rolle.MOR, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10),
            null, null, null, null, null, null, null, null, null, null);

        var resultater = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(arbeidsuke, tomt));

        assertThat(resultater).hasSize(1);
        assertThat(resultater.getFirst().fom()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(resultater.getFirst().tom()).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void mapperAlleLagredeEnumverdier() {
        for (var konto : SøknadsuttaksplanPeriode.KontoType.values()) {
            var periode = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG, konto, null, null, null, null, null, null, null, null, null);
            assertThat(map(periode).kontoType().name()).isEqualTo(konto.name());
        }
        for (var utsettelse : SøknadsuttaksplanPeriode.UtsettelseÅrsak.values()) {
            var periode = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG, null, utsettelse, null,
                utsettelse == SøknadsuttaksplanPeriode.UtsettelseÅrsak.FRI ? SøknadsuttaksplanPeriode.MorsAktivitet.ARBEID : null,
                null, null, null, null, null, null);
            assertThat(map(periode).utsettelseÅrsak().name()).isEqualTo(utsettelse.name());
        }
        for (var overføring : SøknadsuttaksplanPeriode.OverføringÅrsak.values()) {
            var periode = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG, null, null, overføring, null, null, null, null, null, null, null);
            assertThat(map(periode).overføringÅrsak().name()).isEqualTo(overføring.name());
        }
        for (var aktivitet : SøknadsuttaksplanPeriode.AktivitetType.values()) {
            var periode = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG, null, null, null, null,
                BigDecimal.ONE, null, null, aktivitet, null, null);
            assertThat(map(periode).gradering().aktivitet().type().name()).isEqualTo(aktivitet.name());
        }
        for (var morsAktivitet : SøknadsuttaksplanPeriode.MorsAktivitet.values()) {
            var periode = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG, null, null, null, morsAktivitet, null, null, null, null, null, null);
            assertThat(map(periode).morsAktivitet().name()).isEqualTo(morsAktivitet.name());
        }
        for (var arbeidsgiverType : SøknadsuttaksplanPeriode.ArbeidsgiverType.values()) {
            var periode = periode(Søknadsuttaksplan.Rolle.MOR, MANDAG, MANDAG, null, null, null, null,
                BigDecimal.ONE, null, null, SøknadsuttaksplanPeriode.AktivitetType.ANNET, "id", arbeidsgiverType);
            assertThat(map(periode).gradering().aktivitet().arbeidsgiver().type().name()).isEqualTo(arbeidsgiverType.name());
        }
    }

    private static FellesUttaksplanDto.UttakDto map(SøknadsuttaksplanPeriode periode) {
        return UttaksplanMapper.mapLagredeSøknadsperioder(List.of(periode)).getFirst().uttak();
    }

    private static SøknadsuttaksplanPeriode periode(Søknadsuttaksplan.Rolle rolle, LocalDate fom, LocalDate tom,
                                         SøknadsuttaksplanPeriode.KontoType kontoType, SøknadsuttaksplanPeriode.UtsettelseÅrsak utsettelseÅrsak,
                                         SøknadsuttaksplanPeriode.OverføringÅrsak overføringÅrsak, SøknadsuttaksplanPeriode.MorsAktivitet morsAktivitet,
                                         BigDecimal gradering, BigDecimal samtidig, Boolean flerbarnsdager,
                                         SøknadsuttaksplanPeriode.AktivitetType aktivitet, String arbeidsgiverId,
                                         SøknadsuttaksplanPeriode.ArbeidsgiverType arbeidsgiverType) {
        return new SøknadsuttaksplanPeriode(fom, tom, rolle, kontoType, utsettelseÅrsak, overføringÅrsak, morsAktivitet,
            gradering, samtidig, flerbarnsdager, aktivitet, arbeidsgiverId, arbeidsgiverType);
    }
}
