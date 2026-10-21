package no.nav.foreldrepenger.oversikt.søknad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import jakarta.validation.Validation;
import no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer;
import no.nav.foreldrepenger.oversikt.server.konfig.ApiConfig;
import no.nav.foreldrepenger.oversikt.stub.SøknadRepositoryStub;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import tools.jackson.databind.node.ObjectNode;

class SøknadRestTest {

    @Test
    void tilgangsavslagStopperFørMappingOgLagring() {
        var repository = mock(SøknadRepository.class);
        var tilgang = mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class);
        org.mockito.Mockito.doThrow(new no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException(
            no.nav.foreldrepenger.oversikt.tilgangskontroll.LokalFeilKode.IKKE_TILGANG)).when(tilgang).sjekkAtKallErFraSystem();

        assertThatThrownBy(() -> new SøknadRest(new SøknadTjeneste(repository), tilgang).lagre(null))
            .isInstanceOf(no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException.class);
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void lagrerFellesUttaksplanDtoTilInternSøknad() {
        var repository = mock(SøknadRepository.class);
        var mottatt = OffsetDateTime.parse("2026-09-01T12:00:00+02:00");
        var request = parseRequest(validRequest(SøknadType.FØRSTEGANGSSØKNAD));

        new SøknadRest(new SøknadTjeneste(repository), mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class)).lagre(request);

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
        var morsAktivitet = new FellesUttaksplanDto.Aktivitet(FellesUttaksplanDto.Aktivitet.AktivitetType.FRILANS, null, "ikke lagre");
        var farsAktivitet = new FellesUttaksplanDto.Aktivitet(FellesUttaksplanDto.Aktivitet.AktivitetType.ANNET,
            new FellesUttaksplanDto.Arbeidsgiver("org-1", FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.ORGANISASJON), null);
        var mor = new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.MOR, null, null, null,
            new FellesUttaksplanDto.Gradering(new FellesUttaksplanDto.Arbeidstidprosent(new BigDecimal("50")), morsAktivitet),
            null, null, false, new FellesUttaksplanDto.VedtattResultat(true, false, true, FellesUttaksplanDto.VedtattResultat.Årsak.ANNET));
        var far = new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, null, null,
            new FellesUttaksplanDto.Gradering(new FellesUttaksplanDto.Arbeidstidprosent(new BigDecimal("25")), farsAktivitet),
            null, null, false, null);
        var request = new SøknadRest.SøknadRequest(new Saksnummer("123456"), OffsetDateTime.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.FAR_MEDMOR, SøknadType.FØRSTEGANGSSØKNAD, List.of(
                new FellesUttaksplanDto.UttakPeriodeDto(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), mor, null, null),
                new FellesUttaksplanDto.UttakPeriodeDto(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 20), null, far, null)));
        ObjectNode json = DefaultJsonMapper.getJsonMapper().valueToTree(request);
        ((ObjectNode) json.at("/perioder/1/annenPart/gradering/aktivitet/arbeidsgiver")).put("navn", "ikke lagre");
        var repository = mock(SøknadRepository.class);

        new SøknadRest(new SøknadTjeneste(repository), mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class)).lagre(parseRequest(json));

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
    void manglendeOgNullFlerbarnsdagerMapperTilFalse() {
        var json = validRequestJson();
        ((ObjectNode) json.at("/perioder/0/søker")).remove("flerbarnsdager");
        var parsed = parseRequest(json);
        var repository = mock(SøknadRepository.class);

        new SøknadRest(new SøknadTjeneste(repository), mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class)).lagre(parsed);

        var captor = ArgumentCaptor.forClass(Søknad.class);
        verify(repository).lagre(eq(new no.nav.foreldrepenger.oversikt.domene.Saksnummer("123456")), captor.capture());
        assertThat(captor.getValue().perioder().getFirst().flerbarnsdager()).isFalse();
        ((ObjectNode) json.at("/perioder/0/søker")).putNull("flerbarnsdager");
        var explicitNull = parseRequest(json);
        assertThat(explicitNull.perioder().getFirst().søker().flerbarnsdager()).isFalse();
    }

    @Test
    void validererPåkrevdeNestedFelt() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var utenPerioder = new SøknadRest.SøknadRequest(new Saksnummer("123456"),
                OffsetDateTime.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR, SøknadType.FØRSTEGANGSSØKNAD, List.of());
            var manglendeRolle = new SøknadRest.SøknadRequest(new Saksnummer("123456"), OffsetDateTime.parse("2026-09-01T12:00:00Z"),
                null, SøknadType.FØRSTEGANGSSØKNAD, List.of());
            var json = validRequestJson();
            ((ObjectNode) json.at("/perioder/0/søker")).remove("forelder");
            var manglendeNestedRolle = parseRequest(json);

            assertThat(validator.validate(utenPerioder)).isNotEmpty();
            assertThat(validator.validate(manglendeRolle)).isNotEmpty();
            assertThat(validator.validate(manglendeNestedRolle)).isNotEmpty();
        }
    }

    @Test
    void kreverOffsetOgAvviserTomPeriodeliste() {
        var utenOffset = validRequestJson().put("mottattTidspunkt", "2026-09-01T12:00:00");
        assertThatThrownBy(() -> parseRequest(utenOffset))
            .isInstanceOf(RuntimeException.class);
        var tomListe = new SøknadRest.SøknadRequest(new Saksnummer("123456"), OffsetDateTime.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, SøknadType.FØRSTEGANGSSØKNAD, List.of());
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
        var request = new SøknadRest.SøknadRequest(new Saksnummer("123456"), OffsetDateTime.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, SøknadType.FØRSTEGANGSSØKNAD, List.of());
        assertThatThrownBy(() -> new SøknadRest(new SøknadTjeneste(repository), tilgang).lagre(parseRequest(request)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("perioder kan ikke være tom");
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void apiRegistrererSøknadRest() {
        assertThat(new ApiConfig().getClasses()).contains(SøknadRest.class);
    }

    @ParameterizedTest
    @EnumSource(SøknadType.class)
    void søknadstypeFraJsonStyrerLagringUtenTidligereSøknad(SøknadType søknadType) {
        var request = parseRequest(validRequest(søknadType));
        var repository = new SøknadRepositoryStub();
        var tilgang = mock(no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste.class);

        new SøknadRest(new SøknadTjeneste(repository), tilgang).lagre(request);

        assertThat(request.søknadType()).isEqualTo(søknadType);
        assertThat(repository.hentAktivFor(new no.nav.foreldrepenger.oversikt.domene.Saksnummer("123456")).isPresent())
            .isEqualTo(søknadType == SøknadType.FØRSTEGANGSSØKNAD);
    }

    @Test
    void kreverSøknadstypeOgAvviserUkjentVerdi() {
        var json = validRequestJson();
        json.remove("søknadType");
        var utenType = parseRequest(json);
        var nullType = parseRequest(json.putNull("søknadType"));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (var request : List.of(utenType, nullType)) {
                assertThat(factory.getValidator().validate(request))
                    .extracting(v -> v.getPropertyPath().toString()).containsExactly("søknadType");
            }
        }
        assertThatThrownBy(() -> parseRequest(json.put("søknadType", "UKJENT")))
            .isInstanceOf(RuntimeException.class);
    }

    private static SøknadRest.SøknadRequest parseRequest(String json) {
        return DefaultJsonMapper.fromJson(json, SøknadRest.SøknadRequest.class);
    }

    private static SøknadRest.SøknadRequest parseRequest(Object request) {
        return parseRequest(DefaultJsonMapper.getJsonMapper().writeValueAsString(request));
    }

    private static ObjectNode validRequestJson() {
        return DefaultJsonMapper.getJsonMapper().valueToTree(validRequest(SøknadType.FØRSTEGANGSSØKNAD));
    }

    private static SøknadRest.SøknadRequest validRequest(SøknadType søknadType) {
        var arbeidsgiver = new FellesUttaksplanDto.Arbeidsgiver("test-id-123",
            FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.ORGANISASJON);
        var aktivitet = new FellesUttaksplanDto.Aktivitet(FellesUttaksplanDto.Aktivitet.AktivitetType.ORDINÆRT_ARBEID, arbeidsgiver, null);
        var gradering = new FellesUttaksplanDto.Gradering(new FellesUttaksplanDto.Arbeidstidprosent(new BigDecimal("60.5")), aktivitet);
        var uttak = new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.MOR,
            no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.MØDREKVOTE,
            FellesUttaksplanDto.UtsettelseÅrsak.ARBEID, FellesUttaksplanDto.OverføringÅrsak.ALENEOMSORG,
            gradering, no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.UTDANNING,
            new FellesUttaksplanDto.SamtidigUttak(new BigDecimal("20.5")), true, null);
        var periode = new FellesUttaksplanDto.UttakPeriodeDto(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), uttak, null, null);
        return new SøknadRest.SøknadRequest(new Saksnummer("123456"), OffsetDateTime.parse("2026-09-01T12:00:00+02:00"),
            Søknad.Rolle.FAR_MEDMOR, søknadType, List.of(periode));
    }
}
