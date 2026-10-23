package no.nav.foreldrepenger.oversikt.søknad;

import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenpartUbeskyttetAdresse;
import static no.nav.foreldrepenger.oversikt.søknad.Søknad.Rolle.FAR_MEDMOR;
import static no.nav.foreldrepenger.oversikt.søknad.Søknad.Rolle.MOR;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.FEDREKVOTE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.MØDREKVOTE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPerioderTest.periode;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadType.ENDRINGSSØKNAD;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadType.FØRSTEGANGSSØKNAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.fp.Trekkdager;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.Saker;
import no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;
import no.nav.foreldrepenger.oversikt.stub.SøknadRepositoryStub;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTjeneste;

class SøknadTjenesteTest {

    private static final LocalDate DAG = LocalDate.of(2026, 11, 2);
    private static final Instant MOTTATT = Instant.parse("2026-10-01T10:00:00Z");
    private static final LocalDateTime SAK_MOTTATT = LocalDateTime.of(2026, 10, 1, 10, 0);

    private final AktørId søker = AktørId.dummy();
    private final AktørId annenPart = AktørId.dummy();
    private final AktørId barn = AktørId.dummy();
    private final SøknadRepositoryStub søknadRepository = spy(new SøknadRepositoryStub());
    private final RepositoryStub sakRepository = spy(new RepositoryStub());
    private final DummyPersonOppslagSystemTest personOppslag = spy(annenpartUbeskyttetAdresse());
    private final Saker saker = new Saker(sakRepository, myndigInnloggetBruker(AktørId.dummy()), personOppslag);
    private final AnnenPartSakTjeneste annenPartSakTjeneste = spy(new AnnenPartSakTjeneste(saker));
    private final UttaksplanTjeneste uttaksplanTjeneste = spy(new UttaksplanTjeneste(saker, annenPartSakTjeneste,
        søknadRepository, personOppslag));
    private final SøknadTjeneste tjeneste = new SøknadTjeneste(søknadRepository, sakRepository, uttaksplanTjeneste);

    @Test
    void førstegangssøknadErstatterAlleLagredePerioder() {
        var saksnummer = Saksnummer.dummy();
        søknadRepository.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(20), MØDREKVOTE, null)));
        var ny = søknad(MOTTATT.plusSeconds(60), periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null));

        tjeneste.lagre(saksnummer, ny, FØRSTEGANGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(ny);
        verifyNoInteractions(sakRepository, uttaksplanTjeneste, personOppslag);
    }

    @Test
    void førstegangssøknadLagresUtenSak() {
        var saksnummer = Saksnummer.dummy();
        var ny = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null));

        tjeneste.lagre(saksnummer, ny, FØRSTEGANGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(ny);
        verifyNoInteractions(sakRepository, uttaksplanTjeneste, personOppslag);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void vedtakVinnerOverGammelSøknadOgBeggeRollerKappes(boolean harAktivSøknad) {
        var sak = lagreSak(søker, annenPart, Set.of(barn), familiehendelse(), BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.MØDREKVOTE, new Prosent(50))), List.of()));
        lagreSak(annenPart, søker, Set.of(barn), familiehendelse(), BrukerRolle.FAR,
            vedtak(List.of(uttaksperiode(Konto.FEDREKVOTE, new Prosent(50))), List.of()));
        if (harAktivSøknad) {
            søknadRepository.lagre(sak.saksnummer(), søknad(MOTTATT,
                periode(MOR, DAG.minusDays(7), DAG.plusDays(20), FELLESPERIODE, null)));
        }
        var endring = periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null);

        tjeneste.lagre(sak.saksnummer(), søknad(MOTTATT.plusSeconds(60), endring), ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(sak.saksnummer())).contains(søknad(MOTTATT.plusSeconds(60),
            new SøknadPeriode(DAG, DAG.plusDays(9), MOR, MØDREKVOTE, null, null, null, null, new BigDecimal("50.00"), false, null, null, null),
            new SøknadPeriode(DAG, DAG.plusDays(9), FAR_MEDMOR, FEDREKVOTE, null, null, null, null, new BigDecimal("50.00"), false, null, null, null),
            endring));
    }

    @Test
    void planMedNorskUttakOgEøsLagrerBareNorskUttakUtenResultat() {
        var sak = lagreSak(søker, annenPart, Set.of(barn), familiehendelse(), BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.MØDREKVOTE)), List.of(eøsPeriode())));
        var endring = periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null);

        tjeneste.lagre(sak.saksnummer(), søknad(MOTTATT, endring), ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(sak.saksnummer())).contains(søknad(MOTTATT,
            periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null), endring));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void manglendeSakLagresIkkeSelvMedAktivSøknad(boolean harAktivSøknad) {
        var saksnummer = Saksnummer.dummy();
        var aktiv = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null));
        if (harAktivSøknad) {
            søknadRepository.lagre(saksnummer, aktiv);
        }
        var før = søknadRepository.hentAktivFor(saksnummer);
        clearInvocations(søknadRepository);

        tjeneste.lagre(saksnummer, søknad(MOTTATT.plusSeconds(60), periode(MOR, DAG, DAG.plusDays(9), FELLESPERIODE, null)), ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).isEqualTo(før);
        verify(søknadRepository, never()).lagre(any(), any());
        verifyNoInteractions(uttaksplanTjeneste, personOppslag);
    }

    @ParameterizedTest
    @MethodSource("manglendePlangrunnlag")
    void ingenTomEllerKunEøsPlanBeholderRepository(String grunnlag, boolean harAktivSøknad) {
        var hendelse = grunnlag.equals("ingen") ? new FamilieHendelse(null, null, 1, null) : familiehendelse();
        var eøs = grunnlag.equals("eøs") ? List.of(eøsPeriode()) : List.<UttakPeriodeAnnenpartEøs>of();
        var sak = lagreSak(søker, annenPart, Set.of(), hendelse, BrukerRolle.MOR, vedtak(List.of(), eøs));
        if (grunnlag.equals("ingen")) {
            doReturn(Optional.empty()).when(uttaksplanTjeneste).hentFor(sak);
        }
        if (harAktivSøknad) {
            søknadRepository.lagre(sak.saksnummer(), søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null)));
        }
        var før = søknadRepository.hentAktivFor(sak.saksnummer());
        clearInvocations(søknadRepository);

        tjeneste.lagre(sak.saksnummer(), søknad(MOTTATT.plusSeconds(60),
            periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null)), ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(sak.saksnummer())).isEqualTo(før);
        verify(søknadRepository, never()).lagre(any(), any());
        verify(uttaksplanTjeneste).hentFor(same(sak));
    }

    private static Stream<Arguments> manglendePlangrunnlag() {
        return Stream.of("ingen", "tom", "eøs").flatMap(grunnlag -> Stream.of(true, false).map(aktiv -> Arguments.of(grunnlag, aktiv)));
    }

    @Test
    void kjentSakTrengerIkkeFamiliehendelseEllerBarnForÅBrukeEgetVedtak() {
        var sak = lagreSak(søker, null, null, null, BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.MØDREKVOTE)), List.of()));
        var endring = periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null);

        tjeneste.lagre(sak.saksnummer(), søknad(MOTTATT, endring), ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(sak.saksnummer()).orElseThrow().perioder())
            .containsExactly(periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null), endring);
        verify(uttaksplanTjeneste).hentFor(same(sak));
    }

    @Test
    void planUtenHistorikkFørEndringsdatoLagrerInnsendtAlene() {
        var sak = lagreSak(søker, null, Set.of(barn), familiehendelse(), BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.MØDREKVOTE)), List.of()));
        var ny = søknad(MOTTATT, periode(FAR_MEDMOR, DAG, DAG.plusDays(4), FEDREKVOTE, null));

        tjeneste.lagre(sak.saksnummer(), ny, ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(sak.saksnummer())).contains(ny);
    }

    @ParameterizedTest
    @MethodSource("familiehendelser")
    void brukerEksaktSaksEierOgFamiliehendelseIkkeNyereSakForAnnetBarn(FamilieHendelse hendelse, LocalDate forventetDato) {
        var sak = lagreSak(søker, annenPart, Set.of(), hendelse, BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.MØDREKVOTE)), List.of()));
        var annetBarn = lagreSak(søker, AktørId.dummy(), Set.of(AktørId.dummy()),
            new FamilieHendelse(null, DAG.plusYears(1), 1, null), BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.FELLESPERIODE)), List.of()));
        var endring = periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null);

        tjeneste.lagre(sak.saksnummer(), søknad(MOTTATT, endring), ENDRINGSSØKNAD);

        verify(uttaksplanTjeneste).hentFor(same(sak));
        verify(annenPartSakTjeneste).annenPartGjeldendeSakOppgittSøker(søker, annenPart, null, forventetDato);
        assertThat(søknadRepository.hentAktivFor(sak.saksnummer()).orElseThrow().perioder())
            .containsExactly(periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null), endring);
        assertThat(søknadRepository.hentAktivFor(annetBarn.saksnummer())).isEmpty();
    }

    private static Stream<Arguments> familiehendelser() {
        return Stream.of(
            Arguments.of(new FamilieHendelse(DAG.minusYears(2), DAG.minusYears(2).plusDays(7), 1, DAG), DAG),
            Arguments.of(new FamilieHendelse(DAG, DAG.plusDays(7), 1, null), DAG),
            Arguments.of(familiehendelse(), DAG));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void låserTilSaksnummerSelvOmNyereSakMatcherBarnEllerFamiliehendelse(boolean sammeBarn) {
        var sak = lagreSak(søker, null, Set.of(barn), familiehendelse(), BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.MØDREKVOTE)), List.of()));
        var nyereSak = lagreSak(søker, null, Set.of(sammeBarn ? barn : AktørId.dummy()),
            new FamilieHendelse(null, DAG.plusDays(7), 1, null), BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.FELLESPERIODE)), List.of()));
        var annenSøknad = søknad(MOTTATT, periode(MOR, DAG.minusDays(7), DAG.plusDays(18), FELLESPERIODE, null));
        søknadRepository.lagre(nyereSak.saksnummer(), annenSøknad);
        var endring = periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null);
        clearInvocations(sakRepository, søknadRepository);

        tjeneste.lagre(sak.saksnummer(), søknad(MOTTATT.plusSeconds(60), endring), ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(sak.saksnummer())).contains(søknad(MOTTATT.plusSeconds(60),
            periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null), endring));
        verify(uttaksplanTjeneste).hentFor(same(sak));
        verify(sakRepository, never()).hentFor(søker);
        verify(søknadRepository, never()).hentAktivFor(nyereSak.saksnummer());
        verify(søknadRepository, never()).lagre(eq(nyereSak.saksnummer()), any());
        assertThat(søknadRepository.hentAktivFor(nyereSak.saksnummer())).contains(annenSøknad);
    }

    @Test
    void brukerBarnNårFamiliehendelsenManglerDato() {
        var sak = lagreSak(søker, annenPart, Set.of(barn), new FamilieHendelse(null, null, 1, null), BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.MØDREKVOTE)), List.of()));
        var endring = periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null);

        tjeneste.lagre(sak.saksnummer(), søknad(MOTTATT, endring), ENDRINGSSØKNAD);

        verify(uttaksplanTjeneste).hentFor(same(sak));
        verify(annenPartSakTjeneste).annenPartGjeldendeSakOppgittSøker(søker, annenPart, barn, null);
        assertThat(søknadRepository.hentAktivFor(sak.saksnummer()).orElseThrow().perioder())
            .containsExactly(periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null), endring);
    }

    @ParameterizedTest
    @ValueSource(strings = {"plan", "ident", "adresse"})
    void oppslagsfeilPropagererUtenSkriving(String feilsted) throws Exception {
        var sak = lagreSak(søker, annenPart, Set.of(barn), familiehendelse(), BrukerRolle.MOR,
            vedtak(List.of(uttaksperiode(Konto.MØDREKVOTE)), List.of()));
        var aktiv = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), FELLESPERIODE, null));
        søknadRepository.lagre(sak.saksnummer(), aktiv);
        clearInvocations(søknadRepository);
        var feil = new IllegalStateException("Oppslag feilet");
        switch (feilsted) {
            case "plan" -> doThrow(feil).when(uttaksplanTjeneste).hentFor(sak);
            case "ident" -> doThrow(feil).when(personOppslag).fødselsnummer(annenPart);
            case "adresse" -> doThrow(feil).when(personOppslag).adresseBeskyttelse(new Fødselsnummer(annenPart.value()));
            default -> throw new IllegalArgumentException(feilsted);
        }

        assertThatThrownBy(() -> tjeneste.lagre(sak.saksnummer(), søknad(MOTTATT.plusSeconds(60),
            periode(MOR, DAG.plusDays(10), DAG.plusDays(12), MØDREKVOTE, null)), ENDRINGSSØKNAD)).isSameAs(feil);

        assertThat(søknadRepository.hentAktivFor(sak.saksnummer())).contains(aktiv);
        verify(søknadRepository, never()).lagre(any(), any());
    }

    @ParameterizedTest
    @EnumSource(SøknadType.class)
    void søknadSomIkkeErNyereEnnAktivIgnoreres(SøknadType søknadType) {
        var saksnummer = Saksnummer.dummy();
        var aktiv = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null));
        søknadRepository.lagre(saksnummer, aktiv);
        clearInvocations(søknadRepository);

        tjeneste.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(9), FELLESPERIODE, null)), søknadType);
        tjeneste.lagre(saksnummer, søknad(MOTTATT.minusSeconds(60), periode(MOR, DAG, DAG.plusDays(9), FELLESPERIODE, null)), søknadType);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(aktiv);
        verify(søknadRepository, never()).lagre(any(), any());
        verifyNoInteractions(sakRepository, uttaksplanTjeneste, personOppslag);
    }

    @Test
    void manglendeSøknadstypeLagresIkke() {
        var saksnummer = Saksnummer.dummy();
        var ny = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null));

        assertThatThrownBy(() -> tjeneste.lagre(saksnummer, ny, null)).isInstanceOf(NullPointerException.class);
        assertThat(søknadRepository.hentAktivFor(saksnummer)).isEmpty();
    }

    private static Søknad søknad(Instant mottatt, SøknadPeriode... perioder) {
        return new Søknad(mottatt, MOR, List.of(perioder));
    }

    private SakFP0 lagreSak(AktørId eier, AktørId medforelder, Set<AktørId> barna, FamilieHendelse hendelse,
                            BrukerRolle rolle, FpVedtak vedtak) {
        var mottatt = SAK_MOTTATT.plusDays(sakRepository.hentFor(eier).size());
        var legacy = new FpSøknad(SøknadStatus.BEHANDLET, mottatt, Set.of(), Dekningsgrad.HUNDRE, false);
        var sak = new SakFP0(Saksnummer.dummy(), eier, false, Set.of(vedtak), medforelder, hendelse, Set.of(),
            Set.of(legacy), rolle, barna, new Rettigheter(false, false, false), false, mottatt);
        sakRepository.lagre(sak);
        return sak;
    }

    private static FamilieHendelse familiehendelse() {
        return new FamilieHendelse(null, DAG, 1, null);
    }

    private static FpVedtak vedtak(List<Uttaksperiode> perioder, List<UttakPeriodeAnnenpartEøs> eøs) {
        return new FpVedtak(SAK_MOTTATT, perioder, Dekningsgrad.HUNDRE, eøs, null, null);
    }

    private static Uttaksperiode uttaksperiode(Konto konto) {
        return uttaksperiode(konto, Prosent.ZERO);
    }

    private static Uttaksperiode uttaksperiode(Konto konto, Prosent samtidigUttak) {
        var aktivitet = new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.FRILANS, null, null),
            konto, new Trekkdager(15), Prosent.ZERO);
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktivitet), false);
        return new Uttaksperiode(DAG, DAG.plusDays(18), null, null, null, samtidigUttak, false, null, resultat);
    }

    private static UttakPeriodeAnnenpartEøs eøsPeriode() {
        return new UttakPeriodeAnnenpartEøs(DAG.minusDays(7), DAG.minusDays(3), Konto.FEDREKVOTE, BigDecimal.valueOf(5));
    }
}
