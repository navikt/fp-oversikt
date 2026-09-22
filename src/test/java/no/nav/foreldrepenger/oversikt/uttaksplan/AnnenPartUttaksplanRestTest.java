package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;

import jakarta.validation.Validation;
import no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@Execution(ExecutionMode.SAME_THREAD)
class AnnenPartUttaksplanRestTest {

    @AfterEach
    void tearDown() {
        KontekstHolder.fjernKontekst();
    }

    @Test
    void systemressursSkalKunneLagre() {
        settKontekst(IdentType.Systemressurs);
        var repository = mock(AnnenPartUttaksplanRepository.class);
        var rest = new AnnenPartUttaksplanRest(new TilgangKontrollTjeneste(null, null), repository);
        var request = request(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

        assertThatCode(() -> rest.lagre(request)).doesNotThrowAnyException();

        verify(repository).lagre(new no.nav.foreldrepenger.oversikt.domene.Saksnummer(request.saksnummer().value()),
            new AnnenPartUttaksplan(request.mottattTidspunkt(),
                List.of(new UttaksplanTidslinje.Planperiode(request.perioder().getFirst().fom(), request.perioder().getFirst().tom(),
                    request.perioder().getFirst().uttak()))));
    }

    @Test
    void skalFjerneResultatOgAktivitetsdetaljerFørLagring() {
        settKontekst(IdentType.Systemressurs);
        var repository = mock(AnnenPartUttaksplanRepository.class);
        var rest = new AnnenPartUttaksplanRest(new TilgangKontrollTjeneste(null, null), repository);
        var arbeidstid = new FellesUttaksplanDto.Arbeidstidprosent(BigDecimal.valueOf(60));
        var arbeidsgiver = new FellesUttaksplanDto.Arbeidsgiver("999999999", FellesUttaksplanDto.Arbeidsgiver.ArbeidsgiverType.ORGANISASJON);
        var aktivitet = new FellesUttaksplanDto.Aktivitet(FellesUttaksplanDto.Aktivitet.AktivitetType.ORDINÆRT_ARBEID, arbeidsgiver, "Arbeidsgiver");
        var gradering = new FellesUttaksplanDto.Gradering(arbeidstid, aktivitet);
        var resultat = new FellesUttaksplanDto.VedtattResultat(true, false, true, FellesUttaksplanDto.VedtattResultat.Årsak.ANNET);
        var uttak = new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, null, null, gradering, null, null, true,
            resultat);
        var request = new AnnenPartUttaksplanRest.AnnenPartUttaksplanRequest(new Saksnummer("123456"), LocalDateTime.of(2026, 9, 1, 12, 0),
            List.of(new AnnenPartUttaksplanRest.PeriodeRequest(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), uttak)));
        var planCaptor = ArgumentCaptor.forClass(AnnenPartUttaksplan.class);

        rest.lagre(request);

        verify(repository).lagre(eq(new no.nav.foreldrepenger.oversikt.domene.Saksnummer("123456")), planCaptor.capture());
        assertThat(planCaptor.getValue().perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.uttak().resultat()).isNull();
            assertThat(periode.uttak().gradering().aktivitet()).isNull();
            assertThat(periode.uttak().gradering().arbeidstidprosent()).isEqualTo(arbeidstid);
            assertThat(periode.uttak().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.FAR_MEDMOR);
            assertThat(periode.uttak().flerbarnsdager()).isTrue();
        });
    }

    @Test
    void borgerSkalIkkeKunneLagre() {
        settKontekst(IdentType.EksternBruker);
        var repository = mock(AnnenPartUttaksplanRepository.class);
        var rest = new AnnenPartUttaksplanRest(new TilgangKontrollTjeneste(null, null), repository);

        assertThatThrownBy(() -> rest.lagre(request(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void skalValiderePåkrevdeFeltOgDatointervall() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var validator = validatorFactory.getValidator();
            var ugyldigIntervall = request(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1));
            var perioder = new ArrayList<AnnenPartUttaksplanRest.PeriodeRequest>();
            perioder.add(null);
            var manglendeNestedFelt = new AnnenPartUttaksplanRest.AnnenPartUttaksplanRequest(null, null, perioder);
            var uttakUtenForelder = new FellesUttaksplanDto.UttakDto(null, null, null, null, null, null, null, false, null);
            var ugyldigUttak = new AnnenPartUttaksplanRest.AnnenPartUttaksplanRequest(new Saksnummer("123456"), LocalDateTime.now(),
                List.of(new AnnenPartUttaksplanRest.PeriodeRequest(LocalDate.now(), LocalDate.now(), uttakUtenForelder)));

            assertThat(validator.validate(ugyldigIntervall)).extracting("message").contains("tom kan ikke være før fom");
            assertThat(validator.validate(manglendeNestedFelt)).hasSize(3);
            assertThat(validator.validate(ugyldigUttak)).isNotEmpty();
        }
    }

    @Test
    void skalDeserialisereForventetPayload() {
        var json = """
            {
              "saksnummer": "123456",
              "mottattTidspunkt": "2026-09-01T12:00:00",
              "perioder": [{
                "fom": "2026-10-01",
                "tom": "2026-10-10",
                "uttak": {
                  "forelder": "FAR_MEDMOR",
                  "flerbarnsdager": false
                }
              }]
            }
            """;

        var request = DefaultJsonMapper.fromJson(json, AnnenPartUttaksplanRest.AnnenPartUttaksplanRequest.class);

        assertThat(request.saksnummer().value()).isEqualTo("123456");
        assertThat(request.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(periode.uttak().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.FAR_MEDMOR);
        });
    }

    private static AnnenPartUttaksplanRest.AnnenPartUttaksplanRequest request(LocalDate fom, LocalDate tom) {
        var uttak = new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, null, null, null, null, null, false, null);
        return new AnnenPartUttaksplanRest.AnnenPartUttaksplanRequest(new Saksnummer("123456"), LocalDateTime.of(2026, 9, 1, 12, 0),
            List.of(new AnnenPartUttaksplanRest.PeriodeRequest(fom, tom, uttak)));
    }

    private static void settKontekst(IdentType identType) {
        KontekstHolder.fjernKontekst();
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(identType);
        if (IdentType.Systemressurs.equals(identType)) {
            when(kontekst.getUid()).thenReturn("dev-gcp:teamforeldrepenger:fpsoknad");
        }
        KontekstHolder.setKontekst(kontekst);
    }
}
