package no.nav.foreldrepenger.oversikt.søknad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.validation.Validation;
import no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer;
import no.nav.foreldrepenger.oversikt.server.konfig.ApiConfig;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class SøknadRestTest {

    @Test
    void tilgangsavslagStopperFørMappingOgLagring() {
        var repository = mock(SøknadRepository.class);
        var tilgang = mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class);
        org.mockito.Mockito.doThrow(new no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException(
            no.nav.foreldrepenger.oversikt.tilgangskontroll.LokalFeilKode.IKKE_TILGANG)).when(tilgang).sjekkAtKallErFraSystem();

        assertThatThrownBy(() -> new SøknadRest(repository, tilgang).lagre(null))
            .isInstanceOf(no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException.class);
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void lagrerFellesUttaksplanDtoTilInternSøknad() {
        var repository = mock(SøknadRepository.class);
        var mottatt = OffsetDateTime.parse("2026-09-01T12:00:00+02:00");
        var request = parseRequest(validRequestJson());

        new SøknadRest(repository, mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class)).lagre(request);

        var captor = ArgumentCaptor.forClass(Søknad.class);
        verify(repository).lagre(eq(new no.nav.foreldrepenger.oversikt.domene.Saksnummer("123456")), captor.capture());
        var søknad = captor.getValue();
        assertThat(søknad.mottattTidspunkt()).isEqualTo(mottatt.toInstant());
        assertThat(søknad.rolle()).isEqualTo(Søknad.Rolle.FAR_MEDMOR);
        assertThat(søknad.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(periode.tom()).isEqualTo(LocalDate.of(2026, 10, 10));
            assertThat(periode.rolle()).isEqualTo(Søknad.Rolle.MOR);
            assertThat(periode.kontoType()).isEqualTo(SøknadPeriode.KontoType.MØDREKVOTE);
            assertThat(periode.utsettelseÅrsak()).isEqualTo(SøknadPeriode.UtsettelseÅrsak.ARBEID);
            assertThat(periode.overføringÅrsak()).isEqualTo(SøknadPeriode.OverføringÅrsak.ALENEOMSORG);
            assertThat(periode.morsAktivitet()).isEqualTo(SøknadPeriode.MorsAktivitet.UTDANNING);
            assertThat(periode.graderingArbeidstidprosent()).isEqualByComparingTo("60.5");
            assertThat(periode.samtidigUttaksprosent()).isEqualByComparingTo("20.5");
            assertThat(periode.flerbarnsdager()).isTrue();
            assertThat(periode.aktivitetType()).isEqualTo(SøknadPeriode.AktivitetType.ORDINÆRT_ARBEID);
            assertThat(periode.arbeidsgiverId()).isEqualTo("test-id-123");
            assertThat(periode.arbeidsgiverType()).isEqualTo(SøknadPeriode.ArbeidsgiverType.ORGANISASJON);
        });
    }

    @Test
    void ignorererResultatOgArbeidsgivernavnMenBeholderGraderingForBeggeRoller() {
        var json = """
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"FAR_MEDMOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10","uttak":{"forelder":"MOR","resultat":{"resultat":"ikke lagre"},
                "gradering":{"arbeidstidprosent":50,"aktivitet":{"type":"FRILANS","arbeidsgiverNavn":"ikke lagre"}}}},
              {"fom":"2026-10-11","tom":"2026-10-20","uttak":{"forelder":"FAR_MEDMOR",
                "gradering":{"arbeidstidprosent":25,"aktivitet":{"type":"ANNET","arbeidsgiver":{"id":"org-1","type":"ORGANISASJON","navn":"ikke lagre"}}}}}
            ]}
            """;
        var repository = mock(SøknadRepository.class);

        new SøknadRest(repository, mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class)).lagre(parseRequest(json));

        var captor = ArgumentCaptor.forClass(Søknad.class);
        verify(repository).lagre(eq(new no.nav.foreldrepenger.oversikt.domene.Saksnummer("123456")), captor.capture());
        assertThat(captor.getValue().perioder()).extracting(SøknadPeriode::rolle)
            .containsExactly(Søknad.Rolle.MOR, Søknad.Rolle.FAR_MEDMOR);
        assertThat(captor.getValue().perioder()).extracting(SøknadPeriode::graderingArbeidstidprosent)
            .containsExactly(new java.math.BigDecimal("50"), new java.math.BigDecimal("25"));
        assertThat(captor.getValue().perioder().getFirst().aktivitetType()).isEqualTo(SøknadPeriode.AktivitetType.FRILANS);
        assertThat(captor.getValue().perioder().getFirst().arbeidsgiverId()).isNull();
        assertThat(captor.getValue().perioder().get(1).arbeidsgiverId()).isEqualTo("org-1");
    }

    @Test
    void manglendeFlerbarnsdagerMapperTilFalseOgNullAvvisesVedDeserialisering() {
        var json = validRequestJson().replace("\"flerbarnsdager\":true,", "");
        var parsed = parseRequest(json);
        var repository = mock(SøknadRepository.class);

        new SøknadRest(repository, mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class)).lagre(parsed);

        var captor = ArgumentCaptor.forClass(Søknad.class);
        verify(repository).lagre(eq(new no.nav.foreldrepenger.oversikt.domene.Saksnummer("123456")), captor.capture());
        assertThat(captor.getValue().perioder().getFirst().flerbarnsdager()).isFalse();
        var explicitNull = parseRequest(validRequestJson().replace("\"flerbarnsdager\":true", "\"flerbarnsdager\":null"));
        assertThat(explicitNull.perioder().getFirst().uttak().flerbarnsdager()).isFalse();
    }

    @Test
    void validererPåkrevdeNestedFeltOgDatointervall() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var utenPerioder = new SøknadRest.SøknadRequest(new Saksnummer("123456"),
                OffsetDateTime.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR, List.of());
            var manglendeRolle = new SøknadRest.SøknadRequest(new Saksnummer("123456"), OffsetDateTime.parse("2026-09-01T12:00:00Z"),
                null, List.of());
            var manglendeNestedRolle = parseRequest(validRequestJson().replace("\"forelder\":\"MOR\",", ""));
            var ugyldigIntervall = parseRequest(validRequestJson().replace("2026-10-01", "DATE_FOM")
                .replace("2026-10-10", "2026-10-01").replace("DATE_FOM", "2026-10-10"));

            assertThat(validator.validate(utenPerioder)).isNotEmpty();
            assertThat(validator.validate(manglendeRolle)).isNotEmpty();
            assertThat(validator.validate(manglendeNestedRolle)).isNotEmpty();
            assertThat(validator.validate(ugyldigIntervall)).extracting("message").contains("tom kan ikke være før fom");
        }
    }

    @Test
    void kreverOffsetOgAvviserTomPeriodeliste() {
        assertThatThrownBy(() -> parseRequest(validRequestJson().replace("2026-09-01T12:00:00+02:00", "2026-09-01T12:00:00")))
            .isInstanceOf(RuntimeException.class);
        var tomListe = """
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[]}
            """;
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(parseRequest(tomListe))).isNotEmpty();
        }
    }

    @Test
    void avviserTomPeriodelisteIDomenetOgLagrerIkkeTomRequest() {
        assertThatThrownBy(() -> new Søknad(OffsetDateTime.parse("2026-09-01T12:00:00Z").toInstant(), Søknad.Rolle.MOR, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("perioder kan ikke være tom");

        var repository = mock(SøknadRepository.class);
        var tilgang = mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class);
        assertThatThrownBy(() -> new SøknadRest(repository, tilgang).lagre(parseRequest("""
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00Z","rolle":"MOR","perioder":[]}
            """)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("perioder kan ikke være tom");
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void apiRegistrererSøknadRest() {
        assertThat(new ApiConfig().getClasses()).contains(SøknadRest.class);
    }

    private static SøknadRest.SøknadRequest parseRequest(String json) {
        return DefaultJsonMapper.fromJson(json, SøknadRest.SøknadRequest.class);
    }

    private static String validRequestJson() {
        return """
            {"saksnummer":"123456","mottattTidspunkt":"2026-09-01T12:00:00+02:00","rolle":"FAR_MEDMOR","perioder":[
              {"fom":"2026-10-01","tom":"2026-10-10","uttak":{"forelder":"MOR","kontoType":"MØDREKVOTE",
                "utsettelseÅrsak":"ARBEID","overføringÅrsak":"ALENEOMSORG","morsAktivitet":"UTDANNING","samtidigUttak":20.5,
                "flerbarnsdager":true,"gradering":{"arbeidstidprosent":60.5,"aktivitet":{"type":"ORDINÆRT_ARBEID",
                  "arbeidsgiver":{"id":"test-id-123","type":"ORGANISASJON"}}}}}
            ]}
            """;
    }
}
