package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.soknadsplan.Søknadsplan;
import no.nav.foreldrepenger.oversikt.soknadsplan.SøknadsplanPeriode;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

class UttaksplanMapperLagretSøknadsplanTest {
    private static final LocalDate MANDAG = LocalDate.of(2026, 10, 5);

    @Test
    void mapperRolleFelterAktivitetOgArbeidsgiver() {
        var mor = periode(Søknadsplan.Rolle.MOR, MANDAG, MANDAG, SøknadsplanPeriode.KontoType.FELLESPERIODE,
            null, null, null, new BigDecimal("26.75"), null, false, SøknadsplanPeriode.AktivitetType.ORDINÆRT_ARBEID,
            "000000001");
        var far = periode(Søknadsplan.Rolle.FAR_MEDMOR, MANDAG.plusDays(1), MANDAG.plusDays(1),
            SøknadsplanPeriode.KontoType.FEDREKVOTE, null, SøknadsplanPeriode.OverføringÅrsak.ALENEOMSORG,
            SøknadsplanPeriode.MorsAktivitet.UTDANNING, new BigDecimal("50"), null, true, SøknadsplanPeriode.AktivitetType.FRILANS,
            "00000000001");

        var resultater = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(mor, far));

        assertThat(resultater).hasSize(2);
        assertThat(resultater.get(0).uttak().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.MOR);
        assertThat(resultater.get(0).uttak().kontoType()).isEqualTo(no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.FELLESPERIODE);
        assertThat(resultater.get(0).uttak().gradering().aktivitet().type()).isEqualTo(FellesUttaksplanDto.Aktivitet.AktivitetType.ORDINÆRT_ARBEID);
        assertThat(resultater.get(0).uttak().gradering().aktivitet().arbeidsgiver().id()).isEqualTo("000000001");
        assertThat(resultater.get(0).uttak().gradering().aktivitet().arbeidsgiver().type())
            .isEqualTo(FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.ORGANISASJON);
        assertThat(resultater.get(0).uttak().gradering().aktivitet().arbeidsgiverNavn()).isNull();
        assertThat(resultater.get(1).uttak().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.FAR_MEDMOR);
        assertThat(resultater.get(1).uttak().overføringÅrsak()).isEqualTo(FellesUttaksplanDto.OverføringÅrsak.ALENEOMSORG);
        assertThat(resultater.get(1).uttak().morsAktivitet()).isEqualTo(no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.UTDANNING);
        assertThat(resultater.get(1).uttak().flerbarnsdager()).isTrue();
        assertThat(resultater.get(1).uttak().gradering().aktivitet().arbeidsgiver().id()).isEqualTo("00000000001");
        assertThat(resultater.get(1).uttak().gradering().aktivitet().arbeidsgiver().type())
            .isEqualTo(FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.PRIVAT);
    }

    @Test
    void beholderSamtidigUttakOgGraderingMedEksaktProsent() {
        var periode = periode(Søknadsplan.Rolle.MOR, MANDAG, MANDAG,
            SøknadsplanPeriode.KontoType.MØDREKVOTE, SøknadsplanPeriode.UtsettelseÅrsak.ARBEID, null, null,
            new BigDecimal("12.3456789"), new BigDecimal("87.6543219"), false,
            SøknadsplanPeriode.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE, null);

        var uttak = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(periode)).getFirst().uttak();

        assertThat(uttak.utsettelseÅrsak()).isEqualTo(FellesUttaksplanDto.UtsettelseÅrsak.ARBEID);
        assertThat(uttak.gradering().arbeidstidprosent().value()).isEqualByComparingTo("12.3456789");
        assertThat(uttak.gradering().aktivitet().type()).isEqualTo(FellesUttaksplanDto.Aktivitet.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(uttak.gradering().aktivitet().arbeidsgiver()).isNull();
        assertThat(uttak.samtidigUttak().value()).isEqualByComparingTo("87.6543219");
    }

    @Test
    void nullableFelterForblirNull() {
        var periode = periode(Søknadsplan.Rolle.FAR_MEDMOR, MANDAG, MANDAG,
            null, null, null, null, null, null, false, null, null);

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
        var friUten = periode(Søknadsplan.Rolle.MOR, MANDAG, MANDAG, null,
            SøknadsplanPeriode.UtsettelseÅrsak.FRI, null, null, null, null, false, null, null);
        var friMed = periode(Søknadsplan.Rolle.MOR, MANDAG.plusDays(1), MANDAG.plusDays(1), null,
            SøknadsplanPeriode.UtsettelseÅrsak.FRI, null, SøknadsplanPeriode.MorsAktivitet.ARBEID, null, null,
            false, null, null);

        var resultater = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(friUten, friMed));

        assertThat(resultater).hasSize(1);
        assertThat(resultater.getFirst().uttak().utsettelseÅrsak()).isEqualTo(FellesUttaksplanDto.UtsettelseÅrsak.FRI);
    }

    @Test
    void justererDatoerTilVirkedagerOgUtelaterTomtIntervall() {
        var arbeidsuke = periode(Søknadsplan.Rolle.MOR, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12),
            null, null, null, null, null, null, false, null, null);
        var tomt = periode(Søknadsplan.Rolle.MOR, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10),
            null, null, null, null, null, null, false, null, null);

        var resultater = UttaksplanMapper.mapLagredeSøknadsperioder(List.of(arbeidsuke, tomt));

        assertThat(resultater).hasSize(1);
        assertThat(resultater.getFirst().fom()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(resultater.getFirst().tom()).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void mapperAlleLagredeEnumverdier() {
        for (var konto : SøknadsplanPeriode.KontoType.values()) {
            var periode = periode(Søknadsplan.Rolle.MOR, MANDAG, MANDAG, konto, null, null, null, null, null, false, null, null);
            assertThat(map(periode).kontoType().name()).isEqualTo(konto.name());
        }
        for (var utsettelse : SøknadsplanPeriode.UtsettelseÅrsak.values()) {
            var periode = periode(Søknadsplan.Rolle.MOR, MANDAG, MANDAG, null, utsettelse, null,
                utsettelse == SøknadsplanPeriode.UtsettelseÅrsak.FRI ? SøknadsplanPeriode.MorsAktivitet.ARBEID : null,
                null, null, false, null, null);
            assertThat(map(periode).utsettelseÅrsak().name()).isEqualTo(utsettelse.name());
        }
        for (var overføring : SøknadsplanPeriode.OverføringÅrsak.values()) {
            var periode = periode(Søknadsplan.Rolle.MOR, MANDAG, MANDAG, null, null, overføring, null, null, null, false, null, null);
            assertThat(map(periode).overføringÅrsak().name()).isEqualTo(overføring.name());
        }
        for (var aktivitet : SøknadsplanPeriode.AktivitetType.values()) {
            var periode = periode(Søknadsplan.Rolle.MOR, MANDAG, MANDAG, null, null, null, null,
                BigDecimal.ONE, null, false, aktivitet, null);
            assertThat(map(periode).gradering().aktivitet().type().name()).isEqualTo(aktivitet.name());
        }
        for (var morsAktivitet : SøknadsplanPeriode.MorsAktivitet.values()) {
            var periode = periode(Søknadsplan.Rolle.MOR, MANDAG, MANDAG, null, null, null, morsAktivitet, null, null, false, null, null);
            assertThat(map(periode).morsAktivitet().name()).isEqualTo(morsAktivitet.name());
        }
    }

    private static FellesUttaksplanDto.UttakDto map(SøknadsplanPeriode periode) {
        return UttaksplanMapper.mapLagredeSøknadsperioder(List.of(periode)).getFirst().uttak();
    }

    private static SøknadsplanPeriode periode(Søknadsplan.Rolle rolle, LocalDate fom, LocalDate tom,
                                         SøknadsplanPeriode.KontoType kontoType, SøknadsplanPeriode.UtsettelseÅrsak utsettelseÅrsak,
                                         SøknadsplanPeriode.OverføringÅrsak overføringÅrsak, SøknadsplanPeriode.MorsAktivitet morsAktivitet,
                                         BigDecimal gradering, BigDecimal samtidig, boolean flerbarnsdager,
                                          SøknadsplanPeriode.AktivitetType aktivitet, String arbeidsgiverId) {
        return new SøknadsplanPeriode(fom, tom, rolle, kontoType, utsettelseÅrsak, overføringÅrsak, morsAktivitet,
            gradering, samtidig, flerbarnsdager, aktivitet, arbeidsgiverId);
    }
}
