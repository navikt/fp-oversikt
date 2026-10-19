package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.søknad.Søknad;
import no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

class UttaksplanMapperLagretSøknadTest {
    private static final LocalDate MANDAG = LocalDate.of(2026, 10, 5);

    @Test
    void mapperRolleFelterAktivitetOgArbeidsgiver() {
        var mor = periode(Søknad.Rolle.MOR, MANDAG, MANDAG, SøknadPeriode.KontoType.FELLESPERIODE,
            null, null, null, new BigDecimal("26.75"), null, false, SøknadPeriode.AktivitetType.ORDINÆRT_ARBEID,
            "org-123", SøknadPeriode.ArbeidsgiverType.ORGANISASJON);
        var far = periode(Søknad.Rolle.FAR_MEDMOR, MANDAG.plusDays(1), MANDAG.plusDays(1),
            SøknadPeriode.KontoType.FEDREKVOTE, null, SøknadPeriode.OverføringÅrsak.ALENEOMSORG,
            SøknadPeriode.MorsAktivitet.UTDANNING, new BigDecimal("50"), null, true, SøknadPeriode.AktivitetType.FRILANS,
            "person-1", SøknadPeriode.ArbeidsgiverType.PRIVAT);

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
        var periode = periode(Søknad.Rolle.MOR, MANDAG, MANDAG,
            SøknadPeriode.KontoType.MØDREKVOTE, SøknadPeriode.UtsettelseÅrsak.ARBEID, null, null,
            new BigDecimal("12.3456789"), new BigDecimal("87.6543219"), null,
            SøknadPeriode.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null);

        var uttak = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(periode)).getFirst().uttak();

        assertThat(uttak.utsettelseÅrsak()).isEqualTo(FellesUttaksplanDto.UtsettelseÅrsak.ARBEID);
        assertThat(uttak.gradering().arbeidstidprosent().value()).isEqualByComparingTo("12.3456789");
        assertThat(uttak.gradering().aktivitet().type()).isEqualTo(FellesUttaksplanDto.Aktivitet.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(uttak.samtidigUttak().value()).isEqualByComparingTo("87.6543219");
    }

    @Test
    void nullableFelterForblirNull() {
        var periode = periode(Søknad.Rolle.FAR_MEDMOR, MANDAG, MANDAG,
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
        var friUten = periode(Søknad.Rolle.MOR, MANDAG, MANDAG, null,
            SøknadPeriode.UtsettelseÅrsak.FRI, null, null, null, null, null, null, null, null);
        var friMed = periode(Søknad.Rolle.MOR, MANDAG.plusDays(1), MANDAG.plusDays(1), null,
            SøknadPeriode.UtsettelseÅrsak.FRI, null, SøknadPeriode.MorsAktivitet.ARBEID, null, null,
            null, null, null, null);

        var resultater = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(friUten, friMed));

        assertThat(resultater).hasSize(1);
        assertThat(resultater.getFirst().uttak().utsettelseÅrsak()).isEqualTo(FellesUttaksplanDto.UtsettelseÅrsak.FRI);
    }

    @Test
    void justererDatoerTilVirkedagerOgUtelaterTomtIntervall() {
        var arbeidsuke = periode(Søknad.Rolle.MOR, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12),
            null, null, null, null, null, null, null, null, null, null);
        var tomt = periode(Søknad.Rolle.MOR, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10),
            null, null, null, null, null, null, null, null, null, null);

        var resultater = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(arbeidsuke, tomt));

        assertThat(resultater).hasSize(1);
        assertThat(resultater.getFirst().fom()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(resultater.getFirst().tom()).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void mapperAlleLagredeEnumverdier() {
        for (var konto : SøknadPeriode.KontoType.values()) {
            var periode = periode(Søknad.Rolle.MOR, MANDAG, MANDAG, konto, null, null, null, null, null, null, null, null, null);
            assertThat(map(periode).kontoType().name()).isEqualTo(konto.name());
        }
        for (var utsettelse : SøknadPeriode.UtsettelseÅrsak.values()) {
            var periode = periode(Søknad.Rolle.MOR, MANDAG, MANDAG, null, utsettelse, null,
                utsettelse == SøknadPeriode.UtsettelseÅrsak.FRI ? SøknadPeriode.MorsAktivitet.ARBEID : null,
                null, null, null, null, null, null);
            assertThat(map(periode).utsettelseÅrsak().name()).isEqualTo(utsettelse.name());
        }
        for (var overføring : SøknadPeriode.OverføringÅrsak.values()) {
            var periode = periode(Søknad.Rolle.MOR, MANDAG, MANDAG, null, null, overføring, null, null, null, null, null, null, null);
            assertThat(map(periode).overføringÅrsak().name()).isEqualTo(overføring.name());
        }
        for (var aktivitet : SøknadPeriode.AktivitetType.values()) {
            var periode = periode(Søknad.Rolle.MOR, MANDAG, MANDAG, null, null, null, null,
                BigDecimal.ONE, null, null, aktivitet, null, null);
            assertThat(map(periode).gradering().aktivitet().type().name()).isEqualTo(aktivitet.name());
        }
        for (var morsAktivitet : SøknadPeriode.MorsAktivitet.values()) {
            var periode = periode(Søknad.Rolle.MOR, MANDAG, MANDAG, null, null, null, morsAktivitet, null, null, null, null, null, null);
            assertThat(map(periode).morsAktivitet().name()).isEqualTo(morsAktivitet.name());
        }
        for (var arbeidsgiverType : SøknadPeriode.ArbeidsgiverType.values()) {
            var periode = periode(Søknad.Rolle.MOR, MANDAG, MANDAG, null, null, null, null,
                BigDecimal.ONE, null, null, SøknadPeriode.AktivitetType.ANNET, "id", arbeidsgiverType);
            assertThat(map(periode).gradering().aktivitet().arbeidsgiver().type().name()).isEqualTo(arbeidsgiverType.name());
        }
    }

    private static FellesUttaksplanDto.UttakDto map(SøknadPeriode periode) {
        return UttaksplanMapper.mapLagredeSøknadsperioder(List.of(periode)).getFirst().uttak();
    }

    private static SøknadPeriode periode(Søknad.Rolle rolle, LocalDate fom, LocalDate tom,
                                         SøknadPeriode.KontoType kontoType, SøknadPeriode.UtsettelseÅrsak utsettelseÅrsak,
                                         SøknadPeriode.OverføringÅrsak overføringÅrsak, SøknadPeriode.MorsAktivitet morsAktivitet,
                                         BigDecimal gradering, BigDecimal samtidig, Boolean flerbarnsdager,
                                         SøknadPeriode.AktivitetType aktivitet, String arbeidsgiverId,
                                         SøknadPeriode.ArbeidsgiverType arbeidsgiverType) {
        return new SøknadPeriode(fom, tom, rolle, kontoType, utsettelseÅrsak, overføringÅrsak, morsAktivitet,
            gradering, samtidig, flerbarnsdager, aktivitet, arbeidsgiverId, arbeidsgiverType);
    }
}
