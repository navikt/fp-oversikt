package no.nav.foreldrepenger.oversikt.soknadsplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.ws.rs.BadRequestException;
import tools.jackson.databind.node.ObjectNode;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class SøknadsplanMapperTest {

    @Test
    void mapperNullableDataTilInternPeriode() {
        var request = DefaultJsonMapper.fromJson("""
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10","søker":{"forelder":"MOR"}}
            ]}
            """, SøknadsplanRest.SøknadsplanRequest.class);

        var periode = SøknadsplanMapper.tilPerioder(request.perioder()).getFirst();

        assertThat(periode).isInstanceOf(SøknadsplanPeriode.class);
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
        var request = DefaultJsonMapper.fromJson("""
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10",
                "søker":{"forelder":"MOR","kontoType":"MØDREKVOTE"},
                "annenPart":{"forelder":"FAR_MEDMOR","kontoType":"FEDREKVOTE"}}
            ]}
            """, SøknadsplanRest.SøknadsplanRequest.class);

        var perioder = SøknadsplanMapper.tilPerioder(request.perioder());

        assertThat(perioder).extracting(SøknadsplanPeriode::rolle)
            .containsExactly(Søknadsplan.Rolle.MOR, Søknadsplan.Rolle.FAR_MEDMOR);
    }

    @ParameterizedTest(name = "mapper {0} enum {1} med samme navn")
    @MethodSource("alleEnumverdier")
    void mapperAlleEnumverdier(String felt, String verdi, java.util.function.Function<SøknadsplanPeriode, Enum<?>> uthent) {
        var json = """
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10","søker":{"forelder":"MOR","kontoType":"__konto__",
                "utsettelseÅrsak":"__utsettelse__","overføringÅrsak":"__overføring__","morsAktivitet":"__morsAktivitet__",
                "gradering":{"aktivitet":{"type":"__aktivitet__","arbeidsgiver":{"id":"000000001"}}}}}
            ]}
            """.replace("__konto__", "FORELDREPENGER")
            .replace("__utsettelse__", "ARBEID")
            .replace("__overføring__", "ALENEOMSORG")
            .replace("__morsAktivitet__", "ARBEID")
            .replace("__aktivitet__", "FRILANS");
        var feltNavn = switch (felt) {
            case "konto" -> "kontoType";
            case "utsettelse" -> "utsettelseÅrsak";
            case "overføring" -> "overføringÅrsak";
            case "morsAktivitet" -> "morsAktivitet";
            case "aktivitet" -> "type";
            default -> throw new IllegalArgumentException("Ukjent enum-felt " + felt);
        };
        var defaultVerdi = switch (felt) {
            case "konto" -> "FORELDREPENGER";
            case "utsettelse" -> "ARBEID";
            case "overføring" -> "ALENEOMSORG";
            case "morsAktivitet" -> "ARBEID";
            case "aktivitet" -> "FRILANS";
            default -> throw new IllegalArgumentException("Ukjent enum-felt " + felt);
        };
        json = json.replace("\"" + feltNavn + "\":\"" + defaultVerdi + "\"", "\"" + feltNavn + "\":\"" + verdi + "\"");
        var request = DefaultJsonMapper.fromJson(json, SøknadsplanRest.SøknadsplanRequest.class);

        var periode = SøknadsplanMapper.tilPerioder(request.perioder()).getFirst();

        assertThat(uthent.apply(periode).name()).isEqualTo(verdi);
    }

    static Stream<Arguments> alleEnumverdier() {
        return Stream.concat(
            Stream.concat(
                verdier("konto", KontoType.class, SøknadsplanPeriode::kontoType),
                verdier("utsettelse", FellesUttaksplanDto.UtsettelseÅrsak.class, SøknadsplanPeriode::utsettelseÅrsak)),
            Stream.concat(
                Stream.concat(
                    verdier("overføring", FellesUttaksplanDto.OverføringÅrsak.class, SøknadsplanPeriode::overføringÅrsak),
                    verdier("morsAktivitet", MorsAktivitet.class, SøknadsplanPeriode::morsAktivitet)),
                 verdier("aktivitet", FellesUttaksplanDto.Aktivitet.AktivitetType.class, SøknadsplanPeriode::aktivitetType)
                     .filter(a -> !"ANNET".equals(a.get()[1]))));
    }

    @Test
    void lagrerIkkeAktivitetTypeAnnet() {
        var request = DefaultJsonMapper.fromJson("""
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10",
                "søker":{"forelder":"MOR","kontoType":"MØDREKVOTE"},
                "annenPart":{"forelder":"FAR_MEDMOR","kontoType":"FEDREKVOTE","gradering":{"arbeidstidprosent":40,"aktivitet":{"type":"ANNET"}}}}
            ]}
            """, SøknadsplanRest.SøknadsplanRequest.class);

        var annenPart = SøknadsplanMapper.tilPerioder(request.perioder()).get(1);

        assertThat(annenPart.aktivitetType()).isNull();
        assertThat(annenPart.graderingArbeidstidprosent()).isEqualByComparingTo("40");
    }

    @ParameterizedTest
    @ValueSource(strings = {"000000001", "00000000001"})
    void beholderIdUavhengigAvInnsendtType(String id) {
        for (var type : Arrays.asList(null, "ORGANISASJON", "PRIVAT")) {
            var json = requestMedArbeidsgiverId(id);
            ((ObjectNode) json.at("/perioder/0/søker/gradering/aktivitet/arbeidsgiver")).put("type", type);
            var request = DefaultJsonMapper.fromJson(json.toString(), SøknadsplanRest.SøknadsplanRequest.class);

            assertThat(SøknadsplanMapper.tilPerioder(request.perioder()).getFirst().arbeidsgiverId()).isEqualTo(id);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "12345678", "1234567890", "123456789012", "abcdefghi",
        "12345678a", " 000000001", "000000001 ", "+000000001", "１２３４５６７８９", "000000001\n"})
    void avviserUgyldigIdUtenÅTaDenMedIFeilen(String id) {
        var request = DefaultJsonMapper.fromJson(requestMedArbeidsgiverId(id).toString(), SøknadsplanRest.SøknadsplanRequest.class);

        assertThatThrownBy(() -> SøknadsplanMapper.tilPerioder(request.perioder()))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Arbeidsgiver-ID må bestå av 9 eller 11 sifre")
            .hasNoCause();
    }

    @Test
    void tillaterAktivitetUtenArbeidsgiver() {
        var json = requestMedArbeidsgiverId(null);
        ((ObjectNode) json.at("/perioder/0/søker/gradering/aktivitet")).remove("arbeidsgiver");
        var request = DefaultJsonMapper.fromJson(json.toString(), SøknadsplanRest.SøknadsplanRequest.class);

        assertThat(SøknadsplanMapper.tilPerioder(request.perioder()).getFirst().arbeidsgiverId()).isNull();
    }

    private static ObjectNode requestMedArbeidsgiverId(String id) {
        var json = (ObjectNode) DefaultJsonMapper.getJsonMapper().readTree("""
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10","søker":{"forelder":"MOR",
                "gradering":{"aktivitet":{"type":"ORDINÆRT_ARBEID","arbeidsgiver":{}}}}}
            ]}
            """);
        ((ObjectNode) json.at("/perioder/0/søker/gradering/aktivitet/arbeidsgiver")).put("id", id);
        return json;
    }

    private static Stream<Arguments> verdier(String felt, Class<? extends Enum<?>> enumKlasse,
                                              java.util.function.Function<SøknadsplanPeriode, Enum<?>> uthent) {
        return Arrays.stream(enumKlasse.getEnumConstants())
            .map(verdi -> Arguments.of(felt, verdi.name(), uthent));
    }
}
