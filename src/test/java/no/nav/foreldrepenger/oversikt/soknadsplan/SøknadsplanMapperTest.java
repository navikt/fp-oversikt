package no.nav.foreldrepenger.oversikt.soknadsplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.ws.rs.BadRequestException;
import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Aktivitet;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Aktivitet.AktivitetType;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Arbeidsgiver;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Arbeidstidprosent;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Gradering;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.OverføringÅrsak;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Rolle;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UtsettelseÅrsak;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakDto;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakPeriodeDto;

class SøknadsplanMapperTest {

    private static final LocalDate FOM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 10, 10);

    @Test
    void mapperNullableDataTilInternPeriode() {
        var periode = map(søker(uttak(Rolle.MOR, null, null, null, null, null)));

        assertThat(periode.kontoType()).isNull();
        assertThat(periode.utsettelseÅrsak()).isNull();
        assertThat(periode.overføringÅrsak()).isNull();
        assertThat(periode.morsAktivitet()).isNull();
        assertThat(periode.graderingArbeidstidprosent()).isNull();
        assertThat(periode.samtidigUttaksprosent()).isNull();
        assertThat(periode.flerbarnsdager()).isFalse();
        assertThat(periode.aktivitetType()).isNull();
        assertThat(periode.arbeidsgiverId()).isNull();
    }

    @Test
    void pakkerUtBådeSøkerOgAnnenPartFraSammePeriode() {
        var mor = uttak(Rolle.MOR, KontoType.MØDREKVOTE, null, null, null, null);
        var far = uttak(Rolle.FAR_MEDMOR, KontoType.FEDREKVOTE, null, null, null, null);

        var perioder = SøknadsplanMapper.tilPerioder(List.of(new UttakPeriodeDto(FOM, TOM, mor, far, null)));

        assertThat(perioder).extracting(SøknadsplanPeriode::rolle)
            .containsExactly(Søknadsplan.Rolle.MOR, Søknadsplan.Rolle.FAR_MEDMOR);
        assertThat(perioder).extracting(SøknadsplanPeriode::kontoType)
            .containsExactly(SøknadsplanPeriode.KontoType.MØDREKVOTE, SøknadsplanPeriode.KontoType.FEDREKVOTE);
    }

    @ParameterizedTest
    @EnumSource(KontoType.class)
    void mapperKontoType(KontoType kontoType) {
        var periode = map(søker(uttak(Rolle.MOR, kontoType, null, null, null, null)));

        assertThat(periode.kontoType().name()).isEqualTo(kontoType.name());
    }

    @ParameterizedTest
    @EnumSource(UtsettelseÅrsak.class)
    void mapperUtsettelseÅrsak(UtsettelseÅrsak årsak) {
        var periode = map(søker(uttak(Rolle.MOR, null, årsak, null, null, null)));

        assertThat(periode.utsettelseÅrsak().name()).isEqualTo(årsak.name());
    }

    @ParameterizedTest
    @EnumSource(OverføringÅrsak.class)
    void mapperOverføringÅrsak(OverføringÅrsak årsak) {
        var periode = map(søker(uttak(Rolle.MOR, null, null, årsak, null, null)));

        assertThat(periode.overføringÅrsak().name()).isEqualTo(årsak.name());
    }

    @ParameterizedTest
    @EnumSource(MorsAktivitet.class)
    void mapperMorsAktivitet(MorsAktivitet aktivitet) {
        var periode = map(søker(uttak(Rolle.FAR_MEDMOR, null, null, null, null, aktivitet)));

        assertThat(periode.morsAktivitet().name()).isEqualTo(aktivitet.name());
    }

    @ParameterizedTest
    @EnumSource(value = AktivitetType.class, names = "ANNET", mode = EnumSource.Mode.EXCLUDE)
    void mapperAktivitetType(AktivitetType type) {
        var periode = map(søker(uttak(Rolle.MOR, null, null, null, gradering(type, null), null)));

        assertThat(periode.aktivitetType().name()).isEqualTo(type.name());
    }

    @Test
    void lagrerIkkeAktivitetTypeAnnetForNoenRolle() {
        var mor = uttak(Rolle.MOR, null, null, null, gradering(AktivitetType.ANNET, null), null);
        var far = uttak(Rolle.FAR_MEDMOR, null, null, null, gradering(AktivitetType.ANNET, null), null);

        var perioder = SøknadsplanMapper.tilPerioder(List.of(new UttakPeriodeDto(FOM, TOM, mor, far, null)));

        assertThat(perioder).hasSize(2).allSatisfy(periode -> {
            assertThat(periode.aktivitetType()).isNull();
            assertThat(periode.graderingArbeidstidprosent()).isEqualByComparingTo("50");
        });
    }

    @ParameterizedTest
    @MethodSource("gyldigeArbeidsgivere")
    void beholderIdUavhengigAvInnsendtType(String id, ArbeidsgiverType type) {
        var periode = map(søker(medArbeidsgiver(new Arbeidsgiver(id, type))));

        assertThat(periode.arbeidsgiverId()).isEqualTo(id);
    }

    static Stream<Arguments> gyldigeArbeidsgivere() {
        return Stream.of("000000001", "00000000001")
            .flatMap(id -> Stream.of(null, ArbeidsgiverType.ORGANISASJON, ArbeidsgiverType.PRIVAT)
                .map(type -> Arguments.of(id, type)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "12345678", "1234567890", "123456789012", "abcdefghi",
        "12345678a", " 000000001", "000000001 ", "+000000001", "１２３４５６７８９", "000000001\n"})
    void avviserUgyldigIdUtenÅTaDenMedIFeilen(String id) {
        var perioder = List.of(søker(medArbeidsgiver(new Arbeidsgiver(id, ArbeidsgiverType.ORGANISASJON))));

        assertThatThrownBy(() -> SøknadsplanMapper.tilPerioder(perioder))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Arbeidsgiver-ID må bestå av 9 eller 11 sifre")
            .hasNoCause();
    }

    @Test
    void tillaterAktivitetUtenArbeidsgiver() {
        var periode = map(søker(medArbeidsgiver(null)));

        assertThat(periode.aktivitetType()).isEqualTo(SøknadsplanPeriode.AktivitetType.ORDINÆRT_ARBEID);
        assertThat(periode.arbeidsgiverId()).isNull();
    }

    private static SøknadsplanPeriode map(UttakPeriodeDto periode) {
        return SøknadsplanMapper.tilPerioder(List.of(periode)).getFirst();
    }

    private static UttakPeriodeDto søker(UttakDto uttak) {
        return new UttakPeriodeDto(FOM, TOM, uttak, null, null);
    }

    private static UttakDto medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        return uttak(Rolle.MOR, null, null, null, gradering(AktivitetType.ORDINÆRT_ARBEID, arbeidsgiver), null);
    }

    private static Gradering gradering(AktivitetType type, Arbeidsgiver arbeidsgiver) {
        return new Gradering(new Arbeidstidprosent(new BigDecimal("50")), new Aktivitet(type, arbeidsgiver, null));
    }

    private static UttakDto uttak(Rolle rolle, KontoType kontoType, UtsettelseÅrsak utsettelseÅrsak, OverføringÅrsak overføringÅrsak,
                                  Gradering gradering, MorsAktivitet morsAktivitet) {
        return new UttakDto(rolle, kontoType, utsettelseÅrsak, overføringÅrsak, gradering, morsAktivitet, null, false, null);
    }
}
