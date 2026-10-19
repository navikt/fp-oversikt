package no.nav.foreldrepenger.oversikt.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class SøknadMapperTest {

    @Test
    void mapperNullableDataTilInternPeriode() {
        var request = DefaultJsonMapper.fromJson("""
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10","søker":{"forelder":"MOR"}}
            ]}
            """, SøknadRest.SøknadRequest.class);

        var periode = SøknadMapper.tilPerioder(request.perioder()).getFirst();

        assertThat(periode).isInstanceOf(SøknadPeriode.class);
        assertThat(periode.kontoType()).isNull();
        assertThat(periode.utsettelseÅrsak()).isNull();
        assertThat(periode.overføringÅrsak()).isNull();
        assertThat(periode.morsAktivitet()).isNull();
        assertThat(periode.graderingArbeidstidprosent()).isNull();
        assertThat(periode.samtidigUttaksprosent()).isNull();
        assertThat(periode.flerbarnsdager()).isFalse();
        assertThat(periode.aktivitetType()).isNull();
        assertThat(periode.arbeidsgiverId()).isNull();
        assertThat(periode.arbeidsgiverType()).isNull();
    }

    @Test
    void pakkerUtBådeSøkerOgAnnenPartFraSammePeriode() {
        var request = DefaultJsonMapper.fromJson("""
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10",
                "søker":{"forelder":"MOR","kontoType":"MØDREKVOTE"},
                "annenPart":{"forelder":"FAR_MEDMOR","kontoType":"FEDREKVOTE"}}
            ]}
            """, SøknadRest.SøknadRequest.class);

        var perioder = SøknadMapper.tilPerioder(request.perioder());

        assertThat(perioder).extracting(SøknadPeriode::rolle)
            .containsExactly(Søknad.Rolle.MOR, Søknad.Rolle.FAR_MEDMOR);
    }

    @ParameterizedTest(name = "mapper {0} enum {1} med samme navn")
    @MethodSource("alleEnumverdier")
    void mapperAlleEnumverdier(String felt, String verdi, java.util.function.Function<SøknadPeriode, Enum<?>> uthent) {
        var json = """
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10","søker":{"forelder":"MOR","kontoType":"__konto__",
                "utsettelseÅrsak":"__utsettelse__","overføringÅrsak":"__overføring__","morsAktivitet":"__morsAktivitet__",
                "gradering":{"aktivitet":{"type":"__aktivitet__","arbeidsgiver":{"id":"org-1","type":"__arbeidsgiver__"}}}}}
            ]}
            """.replace("__konto__", "FORELDREPENGER")
            .replace("__utsettelse__", "ARBEID")
            .replace("__overføring__", "ALENEOMSORG")
            .replace("__morsAktivitet__", "ARBEID")
            .replace("__aktivitet__", "FRILANS")
            .replace("__arbeidsgiver__", "PRIVAT");
        var feltNavn = switch (felt) {
            case "konto" -> "kontoType";
            case "utsettelse" -> "utsettelseÅrsak";
            case "overføring" -> "overføringÅrsak";
            case "morsAktivitet" -> "morsAktivitet";
            case "aktivitet" -> "type";
            case "arbeidsgiver" -> "type";
            default -> throw new IllegalArgumentException("Ukjent enum-felt " + felt);
        };
        var defaultVerdi = switch (felt) {
            case "konto" -> "FORELDREPENGER";
            case "utsettelse" -> "ARBEID";
            case "overføring" -> "ALENEOMSORG";
            case "morsAktivitet" -> "ARBEID";
            case "aktivitet" -> "FRILANS";
            case "arbeidsgiver" -> "PRIVAT";
            default -> throw new IllegalArgumentException("Ukjent enum-felt " + felt);
        };
        json = json.replace("\"" + feltNavn + "\":\"" + defaultVerdi + "\"", "\"" + feltNavn + "\":\"" + verdi + "\"");
        var request = DefaultJsonMapper.fromJson(json, SøknadRest.SøknadRequest.class);

        var periode = SøknadMapper.tilPerioder(request.perioder()).getFirst();

        assertThat(uthent.apply(periode).name()).isEqualTo(verdi);
    }

    static Stream<Arguments> alleEnumverdier() {
        return Stream.concat(
            Stream.concat(
                verdier("konto", KontoType.class, SøknadPeriode::kontoType),
                verdier("utsettelse", FellesUttaksplanDto.UtsettelseÅrsak.class, SøknadPeriode::utsettelseÅrsak)),
            Stream.concat(
                Stream.concat(
                    verdier("overføring", FellesUttaksplanDto.OverføringÅrsak.class, SøknadPeriode::overføringÅrsak),
                    verdier("morsAktivitet", MorsAktivitet.class, SøknadPeriode::morsAktivitet)),
                Stream.concat(
                    verdier("aktivitet", FellesUttaksplanDto.Aktivitet.AktivitetType.class, SøknadPeriode::aktivitetType),
                    verdier("arbeidsgiver", FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.class, SøknadPeriode::arbeidsgiverType))));
    }

    private static Stream<Arguments> verdier(String felt, Class<? extends Enum<?>> enumKlasse,
                                              java.util.function.Function<SøknadPeriode, Enum<?>> uthent) {
        return Arrays.stream(enumKlasse.getEnumConstants())
            .map(verdi -> Arguments.of(felt, verdi.name(), uthent));
    }
}
