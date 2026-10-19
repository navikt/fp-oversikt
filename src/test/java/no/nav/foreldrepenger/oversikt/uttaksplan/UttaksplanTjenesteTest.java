package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenpartUbeskyttetAdresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
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
import no.nav.foreldrepenger.oversikt.søknad.Søknad;
import no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode;
import no.nav.foreldrepenger.oversikt.søknad.SøknadRepository;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;

class UttaksplanTjenesteTest {

    private final RepositoryStub sakRepository = new RepositoryStub();

    private record Uttaksdag(LocalDate dato, FellesUttaksplanDto.UttakDto søker, FellesUttaksplanDto.UttakDto annenPart) {
    }

    @Test
    void skalBrukeSøkersSøknadsperioderMenIkkeAnnenPartsNårVedtakMangler() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søkersPeriode = søknadsperiode(termindato.minusWeeks(3), termindato.minusDays(1), Konto.FORELDREPENGER_FØR_FØDSEL);
        var annenPartsPeriode = søknadsperiode(termindato.plusWeeks(6), termindato.plusWeeks(8), Konto.FEDREKVOTE);
        lagre(sakUtenVedtak(søker, annenPart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR, søkersPeriode),
            sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, annenPartsPeriode));

        var plan = tjeneste(søker).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(søkersPeriode.fom());
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.annenPart()).isNull();
        });
    }

    @Test
    void skalFjerneArbeidsgiverFraAnnenPartsGradering() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var aktivitet = new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null),
            Konto.FEDREKVOTE, new Trekkdager(5), new Prosent(40));
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET_GRADERING, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktivitet), false);
        var uttaksperiode = new Uttaksperiode(termindato.plusWeeks(6), termindato.plusWeeks(7), null, null, null, Prosent.ZERO, false, null,
            resultat);
        lagre(sakMedVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, uttaksperiode));

        var plan = tjeneste(søker).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.annenPart().gradering().aktivitet()).isNull();
        });
    }

    @Test
    void skalIkkeReturnereAnnenPartsPerioderNårAnnenPartIkkeHarOppgittSøker() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var tredjePart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søkersPeriode = søknadsperiode(termindato.minusWeeks(3), termindato.minusDays(1), Konto.FORELDREPENGER_FØR_FØDSEL);
        var annenPartsPeriode = uttaksperiode(termindato.plusWeeks(6), termindato.plusWeeks(8), Konto.FEDREKVOTE);
        lagre(sakUtenVedtak(søker, annenPart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR, søkersPeriode),
            sakMedVedtak(annenPart, tredjePart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, annenPartsPeriode));

        var plan = tjeneste(søker).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.annenPart()).isNull();
        });
    }

    @Test
    void skalGiSammeResponsForUkjentSakOgManglendeRelasjon() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var tredjePart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søknadRepository = new SøknadRepositoryStub();
        var tjeneste = tjeneste(søker, søknadRepository);
        var ukjentSak = tjeneste.hentFor(søker, annenPart, barn, termindato);

        lagre(sakUtenVedtak(annenPart, tredjePart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(termindato, termindato.plusWeeks(2), Konto.FEDREKVOTE)));
        var manglendeRelasjon = tjeneste.hentFor(søker, annenPart, barn, termindato);
        var skjermetAnnenPart = tjeneste.hentFor(søker, null, barn, termindato);

        assertThat(ukjentSak).isEmpty();
        assertThat(manglendeRelasjon).isEqualTo(ukjentSak);
        assertThat(skjermetAnnenPart).isEqualTo(ukjentSak);
    }

    @Test
    void skalUtledeMetadataFraSøkerensSakNårDenFinnes() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var søkersTermindato = LocalDate.of(2026, 10, 1);
        var annenPartsTermindato = søkersTermindato.plusDays(2);
        lagre(sakUtenVedtak(søker, annenPart, barn, søkersTermindato, 2, Dekningsgrad.ÅTTI, BrukerRolle.MOR,
                søknadsperiode(søkersTermindato, søkersTermindato.plusWeeks(2), Konto.MØDREKVOTE)),
            sakUtenVedtak(annenPart, søker, barn, annenPartsTermindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
                søknadsperiode(annenPartsTermindato, annenPartsTermindato.plusWeeks(2), Konto.FEDREKVOTE)));

        var plan = tjeneste(søker).hentFor(søker, annenPart, barn, søkersTermindato).orElseThrow();

        assertThat(plan.termindato()).isEqualTo(søkersTermindato);
        assertThat(plan.antallBarn()).isEqualTo(2);
        assertThat(plan.dekningsgrad()).isEqualTo(FellesUttaksplanDto.Dekningsgrad.ÅTTI);
    }

    @Test
    void skalUtledeMetadataFraAnnenPartsSakVedFørstegangssøknad() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        lagre(sakUtenVedtak(annenPart, søker, barn, termindato, 2, Dekningsgrad.ÅTTI, BrukerRolle.FAR,
            søknadsperiode(termindato, termindato.plusWeeks(2), Konto.FEDREKVOTE)));

        var plan = tjeneste(søker).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.termindato()).isEqualTo(termindato);
        assertThat(plan.antallBarn()).isEqualTo(2);
        assertThat(plan.dekningsgrad()).isEqualTo(FellesUttaksplanDto.Dekningsgrad.ÅTTI);
        assertThat(plan.perioder()).isEmpty();
    }

    @Test
    void skalByggePlanUtenAnnenPartNårAnnenForelderMangler() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søkersPeriode = søknadsperiode(termindato.minusWeeks(3), termindato.minusDays(1), Konto.FORELDREPENGER_FØR_FØDSEL);
        lagre(sakUtenVedtak(søker, null, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR, søkersPeriode));

        var plan = tjeneste(søker).hentFor(søker, null, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.annenPart()).isNull();
            assertThat(periode.annenPartEøs()).isNull();
        });
    }

    @Test
    void skalBrukeAktivSøknadForEgenSak() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var egenSak = sakUtenVedtak(søker, annenPart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(termindato.minusWeeks(3), termindato.minusDays(1), Konto.FORELDREPENGER_FØR_FØDSEL));
        var annenSak = sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(termindato.plusWeeks(10), termindato.plusWeeks(12), Konto.FEDREKVOTE));
        lagre(egenSak, annenSak);
        var søknadRepository = new SøknadRepositoryStub();
        var egenNyPeriode = nyPeriode(Søknad.Rolle.MOR, termindato.plusWeeks(2), termindato.plusWeeks(3),
            SøknadPeriode.KontoType.MØDREKVOTE, null, null, null);
        søknadRepository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR, List.of(egenNyPeriode)));
        søknadRepository.lagre(annenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.FAR_MEDMOR,
            List.of(nyPeriode(Søknad.Rolle.FAR_MEDMOR, termindato.plusWeeks(14), termindato.plusWeeks(15),
                SøknadPeriode.KontoType.FEDREKVOTE, null, null, null))));

        var plan = tjeneste(søker, søknadRepository).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(termindato.plusWeeks(2));
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.annenPart()).isNull();
        });
    }

    @Test
    void egenMorsSøknadViserBeggeForeldreNårAnnenPartsSakMangler() {
        assertEgenSøknadViserBeggeForeldreUtenAnnenPartsSak(BrukerRolle.MOR);
    }

    @Test
    void egenFarsSøknadViserBeggeForeldreNårAnnenPartsSakMangler() {
        assertEgenSøknadViserBeggeForeldreUtenAnnenPartsSak(BrukerRolle.FAR);
    }

    private void assertEgenSøknadViserBeggeForeldreUtenAnnenPartsSak(BrukerRolle rolle) {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var egenRolle = rolle == BrukerRolle.MOR ? Søknad.Rolle.MOR : Søknad.Rolle.FAR_MEDMOR;
        var annenRolle = rolle == BrukerRolle.MOR ? Søknad.Rolle.FAR_MEDMOR : Søknad.Rolle.MOR;
        var egenKonto = rolle == BrukerRolle.MOR ? SøknadPeriode.KontoType.MØDREKVOTE : SøknadPeriode.KontoType.FEDREKVOTE;
        var annenKonto = rolle == BrukerRolle.MOR ? SøknadPeriode.KontoType.FEDREKVOTE : SøknadPeriode.KontoType.MØDREKVOTE;
        var egenPeriode = rolle == BrukerRolle.MOR ? Konto.MØDREKVOTE : Konto.FEDREKVOTE;
        var egenSak = sakUtenVedtak(søker, annenPart, barn, termindato, 1, Dekningsgrad.HUNDRE, rolle,
            søknadsperiode(termindato, termindato.plusDays(4), egenPeriode));
        lagre(egenSak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            egenRolle, List.of(
                nyPeriode(egenRolle, termindato.plusWeeks(2), termindato.plusWeeks(2).plusDays(4), egenKonto, null, null, null),
                nyPeriode(annenRolle, termindato.plusWeeks(4), termindato.plusWeeks(4).plusDays(4), annenKonto, null, null, null))));
        var tjeneste = tjeneste(søker, repository);

        for (var oppgittAnnenPart : new AktørId[] {null, annenPart}) {
            var plan = tjeneste.hentFor(søker, oppgittAnnenPart, barn, termindato).orElseThrow();

            assertThat(plan.perioder()).hasSize(2);
            assertThat(plan.perioder()).filteredOn(p -> p.søker() != null).singleElement().satisfies(p -> {
                assertThat(p.fom()).isEqualTo(termindato.plusWeeks(2));
            });
            assertThat(plan.perioder()).filteredOn(p -> p.annenPart() != null).singleElement().satisfies(p -> {
                assertThat(p.fom()).isEqualTo(termindato.plusWeeks(4));
                assertThat(p.annenPart().gradering()).isNull();
            });
        }
    }

    @Test
    void manglendeSøknadForEgenSakBrukerEgenLegacy() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var egenSak = sakUtenVedtak(søker, annenPart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(termindato.plusWeeks(1), termindato.plusWeeks(1).plusDays(4), Konto.MØDREKVOTE));
        var annenSak = sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(termindato.plusWeeks(8), termindato.plusWeeks(8).plusDays(4), Konto.FEDREKVOTE));
        lagre(egenSak, annenSak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(annenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.FAR_MEDMOR,
            List.of(nyPeriode(Søknad.Rolle.FAR_MEDMOR, termindato.plusWeeks(10), termindato.plusWeeks(11),
                SøknadPeriode.KontoType.FEDREKVOTE, null, null, null))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.fom()).isEqualTo(termindato.plusWeeks(1));
        });
    }

    @Test
    void aktivSøknadFiltrertTilTomListeGirIkkeLegacyFallback() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var legacyFom = termindato.plusWeeks(2);
        var sak = sakUtenVedtak(søker, null, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(legacyFom, legacyFom.plusDays(4), Konto.MØDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        var filtrertOverlappMedOpphold = new SøknadPeriode(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10), Søknad.Rolle.MOR, null,
            null, null, null, null, null, false, null, null, null);
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, List.of(filtrertOverlappMedOpphold)));

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).isEmpty();
    }

    @Test
    void behandletEgenLegacySøknadBidrarMedUtsettelseMenIkkeUttakNårVedtakFinnes() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var perioder = Set.of(
            søknadsperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE),
            legacyUtsettelse(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4)));
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, LocalDateTime.now(), perioder, Dekningsgrad.HUNDRE, false);
        var vedtaksperiode = uttaksperiode(mandag.plusWeeks(1), mandag.plusWeeks(1).plusDays(4), Konto.MØDREKVOTE);
        var vedtak = new FpVedtak(LocalDateTime.now(), List.of(vedtaksperiode), Dekningsgrad.HUNDRE, null, null, null);
        var sak = sak(søker, null, barn, mandag, 1, BrukerRolle.MOR, Set.of(vedtak), Set.of(søknad));
        lagre(sak);
        var repository = new SøknadRepositoryStub();

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).hasSize(2);
        assertThat(plan.perioder().get(0)).satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(vedtaksperiode.fom());
            assertThat(periode.søker().utsettelseÅrsak()).isNull();
        });
        assertThat(plan.perioder().get(1)).satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(mandag.plusWeeks(2));
            assertThat(periode.søker().utsettelseÅrsak()).isNotNull();
        });
    }

    @Test
    void vedtaksutsettelseVisesOgVikerForAnnenPartsUttak() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var søkersVedtak = new FpVedtak(LocalDateTime.now(), List.of(
            uttaksperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE),
            vedtaksUtsettelse(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4))), Dekningsgrad.HUNDRE, null, null, null);
        var annenPartsUttak = uttaksperiode(mandag.plusWeeks(2).plusDays(2), mandag.plusWeeks(2).plusDays(3), Konto.FEDREKVOTE);
        var tomLegacy = new FpSøknad(SøknadStatus.BEHANDLET, LocalDateTime.now(), Set.of(), Dekningsgrad.HUNDRE, false);
        lagre(
            sak(søker, annenPart, barn, mandag, 1, BrukerRolle.MOR, Set.of(søkersVedtak), Set.of(tomLegacy)),
            sakMedVedtak(annenPart, søker, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, annenPartsUttak));

        var plan = tjeneste(søker).hentFor(søker, annenPart, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2).plusDays(2));
            assertThat(p.annenPart()).isNotNull();
            assertThat(p.søker()).isNull();
        });
        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2));
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
        });
        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2).plusDays(4));
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
        });
    }

    @Test
    void vedtaksutsettelseVisesNårDenIkkeOverlapperAnnenPartsUttak() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var sak = sakMedVedtak(søker, null, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            vedtaksUtsettelse(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4)));
        lagre(sak);

        var plan = tjeneste(søker).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2));
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
            assertThat(p.søker().resultat()).isNotNull();
        });
    }

    @Test
    void legacyUtsettelseFyllerHullBådeMedOgUtenVedtak() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var legacy = new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(
            legacyUtsettelse(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4))), Dekningsgrad.HUNDRE, false);
        var sakUtenVedtak = sak(søker, null, barn, mandag, 1, BrukerRolle.MOR, Set.of(), Set.of(legacy));
        lagre(sakUtenVedtak);

        var legacyPlan = tjeneste(søker).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(legacyPlan.perioder()).singleElement().satisfies(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2));
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
        });

        var søkerMedVedtak = AktørId.dummy();
        var sakMedVedtak = sak(søkerMedVedtak, null, barn, mandag, 1, BrukerRolle.MOR,
            Set.of(new FpVedtak(LocalDateTime.now(), List.of(uttaksperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE)),
                Dekningsgrad.HUNDRE, null, null, null)), Set.of(legacy));
        lagre(sakMedVedtak);

        var vedtaksPlan = tjeneste(søkerMedVedtak).hentFor(søkerMedVedtak, null, barn, mandag).orElseThrow();

        assertThat(vedtaksPlan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2));
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
        });
    }

    @Test
    void søknadsutsettelseOverskriverIkkeVedtaksutsettelse() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var vedtaksutsettelse = vedtaksUtsettelse(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4));
        var sak = sakMedVedtak(søker, null, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR, vedtaksutsettelse);
        lagre(sak);
        var søknadRepository = new SøknadRepositoryStub();
        søknadRepository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR,
            List.of(utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4)))));

        var plan = tjeneste(søker, søknadRepository).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(p -> {
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
            assertThat(p.søker().resultat()).isNotNull();
        });
    }

    @Test
    void søknadsutsettelseFyllerIkkeEøsPeriode() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var eøs = new UttakPeriodeAnnenpartEøs(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4), Konto.FEDREKVOTE,
            new java.math.BigDecimal("5.5"));
        var vedtak = new FpVedtak(LocalDateTime.now(), List.of(), Dekningsgrad.HUNDRE, List.of(eøs), null, null);
        var tomLegacy = new FpSøknad(SøknadStatus.BEHANDLET, LocalDateTime.now(), Set.of(), Dekningsgrad.HUNDRE, false);
        var sak = sak(søker, null, barn, mandag, 1, BrukerRolle.MOR, Set.of(vedtak), Set.of(tomLegacy));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR,
            List.of(utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4)))));

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(p -> {
            assertThat(p.søker()).isNull();
            assertThat(p.annenPartEøs().trekkdager().verdi()).isEqualByComparingTo("5.5");
        });
    }

    @Test
    void annenPartsLegacySøknadBrukesAldriSomPeriodekilde() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var sak = sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(termindato.plusWeeks(2), termindato.plusWeeks(2).plusDays(4), Konto.FEDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).isEmpty();
    }

    @Test
    void ubehandletLegacyUtenVedtakGirUttakOgUtsettelse() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var søknad = new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(
            søknadsperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE),
            legacyUtsettelse(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4))), Dekningsgrad.HUNDRE, false);
        var sak = sak(søker, null, barn, mandag, 1, BrukerRolle.MOR, Set.of(), Set.of(søknad));
        lagre(sak);
        var repository = new SøknadRepositoryStub();

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).hasSize(2);
        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag);
            assertThat(p.søker().utsettelseÅrsak()).isNull();
        });
        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2));
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
        });
    }

    @Test
    void skalBrukeAktivSøknadFraAutorisertAnnenPartsSakNårSøkerManglerSak() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var sak = sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(termindato, termindato.plusDays(4), Konto.FEDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        var aktiv = new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.FAR_MEDMOR,
            List.of(nyPeriode(Søknad.Rolle.MOR, termindato.plusWeeks(2), termindato.plusWeeks(2).plusDays(4),
                SøknadPeriode.KontoType.MØDREKVOTE, new java.math.BigDecimal("40"), SøknadPeriode.AktivitetType.ORDINÆRT_ARBEID, "org-1"),
                nyPeriode(Søknad.Rolle.FAR_MEDMOR, termindato.plusWeeks(3), termindato.plusWeeks(3).plusDays(4),
                    SøknadPeriode.KontoType.FEDREKVOTE, null, null, null)));
        repository.lagre(sak.saksnummer(), aktiv);

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).hasSize(2);
        assertThat(plan.perioder()).filteredOn(periode -> periode.søker() != null).singleElement().satisfies(periode -> {
            assertThat(periode.søker().gradering().aktivitet().arbeidsgiver().id()).isEqualTo("org-1");
        });
        assertThat(plan.perioder()).anySatisfy(periode -> {
            assertThat(periode.annenPart()).isNotNull();
            assertThat(periode.annenPart().forelder()).isEqualTo(no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.Rolle.FAR_MEDMOR);
        });
    }

    @Test
    void brukerAnnenPartsVedtakOgSøknadNårSøkerManglerSak() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var annenPartsVedtak = uttaksperiode(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4), Konto.FEDREKVOTE);
        var sak = sakMedVedtak(annenPart, søker, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, annenPartsVedtak);
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.FAR_MEDMOR, List.of(
                nyPeriode(Søknad.Rolle.FAR_MEDMOR, mandag, mandag.plusDays(4), SøknadPeriode.KontoType.FEDREKVOTE, null, null, null),
                nyPeriode(Søknad.Rolle.MOR, mandag.plusWeeks(4), mandag.plusWeeks(4).plusDays(4),
                    SøknadPeriode.KontoType.MØDREKVOTE, null, null, null))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).hasSize(2);
        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(annenPartsVedtak.fom());
            assertThat(p.annenPart()).isNotNull();
        });
        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(4));
            assertThat(p.søker()).isNotNull();
        });
        assertThat(plan.perioder()).noneMatch(p -> p.fom().equals(mandag));
    }

    @Test
    void annenPartsSøknadMedUkjentRolleFeilerLukketNårSøkerManglerSak() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var sak = sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.UKJENT,
            søknadsperiode(termindato, termindato.plusDays(4), Konto.FEDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.FAR_MEDMOR, List.of(nyPeriode(Søknad.Rolle.MOR, termindato, termindato.plusDays(4),
                SøknadPeriode.KontoType.MØDREKVOTE, null, null, null))));

        assertThatThrownBy(() -> tjeneste(søker, repository).hentFor(søker, annenPart, barn, termindato))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Ugyldig rolle i uttaksplan");
    }

    @Test
    void annenPartsSøknadMedRollemismatchBrukerBareVedtakNårSøkerManglerSak() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var aktivitet = new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null),
            Konto.FEDREKVOTE, new Trekkdager(5), new Prosent(40));
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET_GRADERING, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktivitet), false);
        var vedtaksperiode = new Uttaksperiode(termindato.plusWeeks(2), termindato.plusWeeks(2).plusDays(4), null, null, null,
            Prosent.ZERO, false, null, resultat);
        var sak = sakMedVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, vedtaksperiode);
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, List.of(
                nyPeriode(Søknad.Rolle.MOR, termindato, termindato.plusDays(4), SøknadPeriode.KontoType.MØDREKVOTE,
                    new java.math.BigDecimal("50"), SøknadPeriode.AktivitetType.ORDINÆRT_ARBEID, "other-org"),
                nyPeriode(Søknad.Rolle.FAR_MEDMOR, termindato.plusWeeks(1), termindato.plusWeeks(1).plusDays(4),
                    SøknadPeriode.KontoType.FEDREKVOTE, null, null, null))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(vedtaksperiode.fom());
            assertThat(periode.annenPart()).isNotNull();
            assertThat(periode.søker()).isNull();
            assertThat(periode.annenPart().gradering().aktivitet()).isNull();
        });
    }

    @Test
    void vedtakMedOverlappendeForkastedeSøknadsuttakBlokkererIkkeUtsettelse() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var sak = sakMedVedtak(søker, null, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            uttaksperiode(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4), Konto.MØDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, List.of(
                nyPeriode(Søknad.Rolle.MOR, mandag, mandag.plusDays(4), SøknadPeriode.KontoType.MØDREKVOTE, null, null, null),
                nyPeriode(Søknad.Rolle.MOR, mandag.plusDays(2), mandag.plusDays(6), SøknadPeriode.KontoType.FELLESPERIODE, null, null, null),
                utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusDays(8), mandag.plusDays(12)))));

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusDays(8));
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
        });
    }

    @Test
    void vedtaksperioderForkasterOverlappendeUttakFraValgtSøknadOgKlippeEierensUtsettelse() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var egenSak = sakUtenVedtak(søker, annenPart, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE));
        var annenSak = sakMedVedtak(annenPart, søker, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            uttaksperiode(mandag.plusDays(10), mandag.plusDays(12), Konto.FEDREKVOTE));
        lagre(egenSak, annenSak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, List.of(
                nyPeriode(Søknad.Rolle.MOR, mandag, mandag.plusDays(4), SøknadPeriode.KontoType.MØDREKVOTE, null, null, null),
                utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusDays(2), mandag.plusDays(14)))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).anySatisfy(p -> assertThat(p.fom()).isEqualTo(mandag));
        assertThat(plan.perioder()).filteredOn(p -> p.søker() != null && p.søker().utsettelseÅrsak() != null)
            .extracting(p -> p.fom()).containsExactly(mandag.plusDays(7), mandag.plusDays(14));
        assertThat(plan.perioder()).noneMatch(p -> p.annenPart() != null && p.annenPart().utsettelseÅrsak() != null);
    }

    @Test
    void vedtakMedTomtMappetUttakBrukerIkkeSøknadsuttak() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var lørdag = LocalDate.of(2026, 10, 10);
        var sak = sakMedVedtak(søker, null, barn, lørdag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            uttaksperiode(lørdag, lørdag, Konto.MØDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, List.of(nyPeriode(Søknad.Rolle.MOR, lørdag.plusWeeks(2), lørdag.plusWeeks(2).plusDays(4),
                SøknadPeriode.KontoType.MØDREKVOTE, null, null, null))));

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, lørdag).orElseThrow();

        assertThat(plan.perioder()).isEmpty();
    }

    @Test
    void uttakFraBeggeVedtakPrioriteresOverHeleEllerDelerAvSøknadseierensUtsettelse() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var egenSak = sakMedVedtak(søker, annenPart, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            uttaksperiode(mandag.plusDays(2), mandag.plusDays(3), Konto.MØDREKVOTE));
        var annenSak = sakMedVedtak(annenPart, søker, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            uttaksperiode(mandag.plusDays(8), mandag.plusDays(9), Konto.FEDREKVOTE));
        lagre(egenSak, annenSak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR,
            List.of(utsettelsePeriode(Søknad.Rolle.MOR, mandag, mandag.plusDays(11)))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).extracting(p -> p.fom()).containsExactly(
            mandag, mandag.plusDays(2), mandag.plusDays(4), mandag.plusDays(8), mandag.plusDays(10));
        assertThat(plan.perioder()).filteredOn(p -> p.søker() != null && p.søker().utsettelseÅrsak() != null)
            .extracting(p -> p.fom()).containsExactly(mandag, mandag.plusDays(4), mandag.plusDays(10));
        assertThat(plan.perioder()).filteredOn(p -> p.søker() != null && p.søker().utsettelseÅrsak() == null)
            .singleElement().satisfies(p -> assertThat(p.fom()).isEqualTo(mandag.plusDays(2)));
        assertThat(plan.perioder()).noneMatch(p -> p.annenPart() != null && p.annenPart().utsettelseÅrsak() != null);
        assertThat(plan.perioder()).filteredOn(p -> p.annenPart() != null && p.annenPart().utsettelseÅrsak() == null)
            .singleElement().satisfies(p -> assertThat(p.fom()).isEqualTo(mandag.plusDays(8)));
    }

    @Test
    void søknadsutsettelserEndrerAldriUttak() {
        var planUtenUtsettelser = lagPlanMedSøknadsutsettelser(false);
        var planMedUtsettelser = lagPlanMedSøknadsutsettelser(true);

        assertThat(planMedUtsettelser.perioder()).anyMatch(p -> p.søker() != null && p.søker().utsettelseÅrsak() != null
            || p.annenPart() != null && p.annenPart().utsettelseÅrsak() != null);
        assertThat(uttaksdager(planMedUtsettelser)).isEqualTo(uttaksdager(planUtenUtsettelser));
    }

    private FellesUttaksplanDto lagPlanMedSøknadsutsettelser(boolean medUtsettelser) {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var søkersUttak = List.of(
            uttaksperiode(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4), Konto.MØDREKVOTE),
            uttaksperiode(mandag.plusWeeks(6), mandag.plusWeeks(6).plusDays(4), Konto.MØDREKVOTE),
            uttaksperiode(mandag.plusWeeks(10), mandag.plusWeeks(10).plusDays(4), Konto.MØDREKVOTE));
        var annenPartsUttak = List.of(
            uttaksperiode(mandag.plusWeeks(4), mandag.plusWeeks(4).plusDays(4), Konto.FEDREKVOTE),
            uttaksperiode(mandag.plusWeeks(8), mandag.plusWeeks(8).plusDays(4), Konto.FEDREKVOTE),
            uttaksperiode(mandag.plusWeeks(12), mandag.plusWeeks(12).plusDays(4), Konto.FEDREKVOTE));
        var lagretSøknad = new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(),
            Set.of(søknadsperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE)), Dekningsgrad.HUNDRE, false);
        var egenSak = sak(søker, annenPart, barn, mandag, 1, BrukerRolle.MOR,
            Set.of(new FpVedtak(LocalDateTime.now(), søkersUttak, Dekningsgrad.HUNDRE, null, null, null)), Set.of(lagretSøknad));
        var annenSak = sak(annenPart, søker, barn, mandag, 1, BrukerRolle.FAR,
            Set.of(new FpVedtak(LocalDateTime.now(), annenPartsUttak, Dekningsgrad.HUNDRE, null, null, null)), Set.of(lagretSøknad));
        var repository = new RepositoryStub();
        repository.lagre(egenSak);
        repository.lagre(annenSak);
        var søknadRepository = new SøknadRepositoryStub();
        var perioder = new java.util.ArrayList<SøknadPeriode>();
        perioder.add(nyPeriode(Søknad.Rolle.MOR, mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4),
            SøknadPeriode.KontoType.MØDREKVOTE, null, null, null));
        perioder.add(nyPeriode(Søknad.Rolle.FAR_MEDMOR, mandag.plusWeeks(4), mandag.plusWeeks(4).plusDays(4),
            SøknadPeriode.KontoType.FEDREKVOTE, null, null, null));
        if (medUtsettelser) {
            perioder.addAll(List.of(
                utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(2).plusDays(2), mandag.plusWeeks(3).plusDays(1)),
                utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(4).plusDays(2), mandag.plusWeeks(5).plusDays(1)),
                utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(6).plusDays(1), mandag.plusWeeks(6).plusDays(3)),
                utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(7), mandag.plusWeeks(7).plusDays(4)),
                utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(10).plusDays(2), mandag.plusWeeks(11).plusDays(1)),
                utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(12).plusDays(2), mandag.plusWeeks(13).plusDays(1))));
        }
        søknadRepository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR, perioder));

        return tjeneste(søker, søknadRepository, repository).hentFor(søker, annenPart, barn, mandag).orElseThrow();
    }

    private static List<Uttaksdag> uttaksdager(FellesUttaksplanDto plan) {
        return plan.perioder().stream().flatMap(periode -> {
            var søkerUttak = periode.søker() != null && periode.søker().utsettelseÅrsak() == null ? periode.søker() : null;
            var annenPartsUttak = periode.annenPart() != null && periode.annenPart().utsettelseÅrsak() == null ? periode.annenPart() : null;
            if (søkerUttak == null && annenPartsUttak == null) {
                return java.util.stream.Stream.empty();
            }
            return periode.fom().datesUntil(periode.tom().plusDays(1))
                .filter(dato -> dato.getDayOfWeek().getValue() <= 5)
                .map(dato -> new Uttaksdag(dato, søkerUttak, annenPartsUttak));
        }).toList();
    }

    @Test
    void farSomSøkerBeholderEgneArbeidsopplysningerOgSkjulerMors() {
        var far = AktørId.dummy();
        var mor = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var egenSak = sakUtenVedtak(far, mor, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MEDMOR,
            søknadsperiode(mandag, mandag.plusDays(4), Konto.FEDREKVOTE));
        var morsSak = sakUtenVedtak(mor, far, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE));
        lagre(egenSak, morsSak);
        var egenPeriode = nyPeriode(Søknad.Rolle.FAR_MEDMOR, mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4),
            SøknadPeriode.KontoType.FEDREKVOTE, new java.math.BigDecimal("25"), SøknadPeriode.AktivitetType.ORDINÆRT_ARBEID, "far-org");
        var morsPeriode = nyPeriode(Søknad.Rolle.MOR, mandag.plusWeeks(3), mandag.plusWeeks(3).plusDays(4),
            SøknadPeriode.KontoType.MØDREKVOTE, new java.math.BigDecimal("50"), SøknadPeriode.AktivitetType.ORDINÆRT_ARBEID, "mor-org");
        var repository = new SøknadRepositoryStub();
        repository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.FAR_MEDMOR,
            List.of(egenPeriode, morsPeriode)));

        var plan = tjeneste(far, repository).hentFor(far, mor, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).filteredOn(p -> p.søker() != null).singleElement().satisfies(p -> {
            assertThat(p.søker().gradering().aktivitet().arbeidsgiver().id()).isEqualTo("far-org");
        });
        assertThat(plan.perioder()).filteredOn(p -> p.annenPart() != null).singleElement().satisfies(p -> {
            assertThat(p.annenPart().gradering().aktivitet()).isNull();
        });
    }

    @Test
    void søknadMedRollemismatchBrukerBareVedtakOgIkkeLegacyEllerAndreForelder() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var vedtaksperiode = uttaksperiode(termindato.plusWeeks(3), termindato.plusWeeks(3).plusDays(4), Konto.MØDREKVOTE);
        var gammelSøknadsperiode = søknadsperiode(termindato.plusWeeks(5), termindato.plusWeeks(5).plusDays(4), Konto.MØDREKVOTE);
        var egenLegacy = new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(gammelSøknadsperiode), Dekningsgrad.HUNDRE, false);
        var egenSak = sak(søker, annenPart, barn, termindato, 1, BrukerRolle.MOR, Set.of(
            new FpVedtak(LocalDateTime.now(), List.of(vedtaksperiode), Dekningsgrad.HUNDRE, null, null, null)), Set.of(egenLegacy));
        var annenPartsSak = sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(termindato.plusWeeks(7), termindato.plusWeeks(7).plusDays(4), Konto.FEDREKVOTE));
        lagre(egenSak, annenPartsSak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(annenPartsSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.FAR_MEDMOR,
            List.of(nyPeriode(Søknad.Rolle.FAR_MEDMOR, termindato.plusWeeks(9), termindato.plusWeeks(10),
                SøknadPeriode.KontoType.FEDREKVOTE, null, null, null))));
        repository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.FAR_MEDMOR, List.of(
                nyPeriode(Søknad.Rolle.FAR_MEDMOR, termindato, termindato.plusDays(4), SøknadPeriode.KontoType.FEDREKVOTE, null, null, null),
                nyPeriode(Søknad.Rolle.MOR, termindato.plusWeeks(2), termindato.plusWeeks(2).plusDays(4),
                    SøknadPeriode.KontoType.MØDREKVOTE, null, null, null))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(vedtaksperiode.fom());
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.annenPart()).isNull();
        });
    }

    @Test
    void egenSøknadMedRollemismatchUtenVedtakGirTomPlan() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var sak = sakUtenVedtak(søker, null, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(termindato, termindato.plusDays(4), Konto.MØDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.FAR_MEDMOR, List.of(nyPeriode(Søknad.Rolle.FAR_MEDMOR, termindato, termindato.plusDays(4),
                SøknadPeriode.KontoType.FEDREKVOTE, null, null, null))));

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).isEmpty();
    }

    @Test
    void ukjentRollePåMatchendeSakSkalFeileLukket() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        lagre(sakUtenVedtak(søker, null, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.UKJENT,
            søknadsperiode(termindato, termindato.plusDays(4), Konto.MØDREKVOTE)));

        assertThatThrownBy(() -> tjeneste(søker).hentFor(søker, null, barn, termindato))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Ugyldig rolle i uttaksplan");
    }

    @Test
    void vedtaksuttakSplitterSøknadsutsettelseVedDatooverlapp() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var sak = sakMedVedtak(søker, null, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            uttaksperiode(mandag.plusDays(2), mandag.plusDays(3), Konto.MØDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR,
            List.of(utsettelsePeriode(Søknad.Rolle.MOR, mandag, mandag.plusDays(6)))));

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).extracting(p -> p.fom()).containsExactly(mandag, mandag.plusDays(2), mandag.plusDays(4));
        assertThat(plan.perioder().get(0).søker().utsettelseÅrsak()).isNotNull();
        assertThat(plan.perioder().get(1).søker().utsettelseÅrsak()).isNull();
        assertThat(plan.perioder().get(2).søker().utsettelseÅrsak()).isNotNull();
    }

    @Test
    void vedtakEøsFjernerBareEgenAktivSøknadsutsettelseSomOverlapper() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var eøsFom = mandag.plusDays(2);
        var eøsTom = mandag.plusDays(3);
        var eøsTrekkdager = new java.math.BigDecimal("2.5");
        var egenSak = sakMedVedtak(søker, annenPart, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            uttaksperiode(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4), Konto.MØDREKVOTE));
        var annenSak = sakUtenVedtak(annenPart, søker, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(mandag.plusWeeks(6), mandag.plusWeeks(6).plusDays(4), Konto.FEDREKVOTE));
        var vedtak = new FpVedtak(LocalDateTime.now(), List.of(uttaksperiode(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4), Konto.MØDREKVOTE)),
            Dekningsgrad.HUNDRE, List.of(new UttakPeriodeAnnenpartEøs(eøsFom, eøsTom, Konto.FEDREKVOTE, eøsTrekkdager)), null, null);
        var sakMedEøs = new SakFP0(egenSak.saksnummer(), søker, false, Set.of(vedtak), annenPart, egenSak.familieHendelse(), Set.of(),
            egenSak.søknader(), BrukerRolle.MOR, egenSak.fødteBarn(), new Rettigheter(false, false, false), false, LocalDateTime.now());
        lagre(sakMedEøs, annenSak);
        var repository = new SøknadRepositoryStub();
        var utsettelseFom = mandag;
        var utsettelseTom = mandag.plusDays(4);
        repository.lagre(annenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.FAR_MEDMOR,
            List.of(nyPeriode(Søknad.Rolle.FAR_MEDMOR, mandag.plusWeeks(8), mandag.plusWeeks(9),
                SøknadPeriode.KontoType.FEDREKVOTE, null, null, null))));
        repository.lagre(sakMedEøs.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, List.of(utsettelsePeriode(Søknad.Rolle.MOR, utsettelseFom, utsettelseTom))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).anySatisfy(periode -> {
            assertThat(periode.fom()).isEqualTo(utsettelseFom);
            assertThat(periode.tom()).isEqualTo(eøsFom.minusDays(1));
            assertThat(periode.søker().utsettelseÅrsak()).isNotNull();
        });
        assertThat(plan.perioder()).anySatisfy(periode -> {
            assertThat(periode.fom()).isEqualTo(eøsFom);
            assertThat(periode.tom()).isEqualTo(eøsTom);
            assertThat(periode.søker()).isNull();
            assertThat(periode.annenPartEøs().trekkdager().verdi()).isEqualByComparingTo(eøsTrekkdager);
        });
        assertThat(plan.perioder()).anySatisfy(periode -> {
            assertThat(periode.fom()).isEqualTo(eøsTom.plusDays(1));
            assertThat(periode.tom()).isEqualTo(utsettelseTom);
            assertThat(periode.søker().utsettelseÅrsak()).isNotNull();
        });
        assertThat(plan.perioder()).noneMatch(periode -> periode.fom().isAfter(mandag.plusWeeks(6)));
        assertThat(plan.perioder()).filteredOn(periode -> periode.annenPartEøs() != null)
            .extracting(periode -> periode.annenPartEøs().trekkdager().verdi()).containsExactly(eøsTrekkdager);
    }

    @Test
    void beggeVedtakForkasterAlleSøknadsuttakMenBeholderSøknadseierensUtsettelseUtenforOverlapp() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var egenSak = sakMedVedtak(søker, annenPart, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            uttaksperiode(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4), Konto.MØDREKVOTE));
        var annenSak = sakMedVedtak(annenPart, søker, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            uttaksperiode(mandag.plusWeeks(4), mandag.plusWeeks(4).plusDays(4), Konto.FEDREKVOTE));
        lagre(egenSak, annenSak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR, List.of(
            nyPeriode(Søknad.Rolle.MOR, mandag, mandag.plusDays(4), SøknadPeriode.KontoType.MØDREKVOTE, null, null, null),
            nyPeriode(Søknad.Rolle.FAR_MEDMOR, mandag.plusWeeks(6), mandag.plusWeeks(6).plusDays(4),
                SøknadPeriode.KontoType.FEDREKVOTE, null, null, null),
            utsettelsePeriode(Søknad.Rolle.MOR, mandag.plusWeeks(3), mandag.plusWeeks(3).plusDays(4)))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).hasSize(3);
        assertThat(plan.perioder()).noneMatch(p -> p.fom().equals(mandag) || p.fom().equals(mandag.plusWeeks(6)));
        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2));
            assertThat(p.søker()).isNotNull();
            assertThat(p.søker().kontoType()).isEqualTo(no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType.MØDREKVOTE);
        });
        assertThat(plan.perioder()).anySatisfy(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(4));
            assertThat(p.annenPart()).isNotNull();
        });
        assertThat(plan.perioder()).filteredOn(p -> p.søker() != null && p.søker().utsettelseÅrsak() != null).hasSize(1);
        assertThat(plan.perioder()).noneMatch(p -> p.annenPart() != null && p.annenPart().utsettelseÅrsak() != null);
    }

    @Test
    void søkerUtenSakArverAnnenPartsUtsettelserISøkersFelt() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var sak = sakUtenVedtak(annenPart, søker, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(mandag, mandag.plusDays(4), Konto.FEDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.FAR_MEDMOR,
            List.of(utsettelsePeriode(Søknad.Rolle.FAR_MEDMOR, mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4)))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(p -> {
            assertThat(p.fom()).isEqualTo(mandag.plusWeeks(2));
            assertThat(p.annenPart()).isNull();
            assertThat(p.søker().utsettelseÅrsak()).isNotNull();
            assertThat(p.søker().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.MOR);
        });
    }

    @Test
    void søkerMedEgenSakSerIkkeAnnenPartsUtsettelser() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var egenSak = sakUtenVedtak(søker, annenPart, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE));
        var annenSak = sakUtenVedtak(annenPart, søker, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(mandag.plusWeeks(2), mandag.plusWeeks(2).plusDays(4), Konto.FEDREKVOTE));
        lagre(egenSak, annenSak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(egenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.MOR,
            List.of(nyPeriode(Søknad.Rolle.MOR, mandag, mandag.plusDays(4), SøknadPeriode.KontoType.MØDREKVOTE, null, null, null))));
        repository.lagre(annenSak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"), Søknad.Rolle.FAR_MEDMOR,
            List.of(utsettelsePeriode(Søknad.Rolle.FAR_MEDMOR, mandag.plusWeeks(3), mandag.plusWeeks(3).plusDays(4)))));

        var plan = tjeneste(søker, repository).hentFor(søker, annenPart, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).anySatisfy(p -> assertThat(p.søker()).isNotNull());
        assertThat(plan.perioder()).noneMatch(p -> p.annenPart() != null && p.annenPart().utsettelseÅrsak() != null);
    }

    @Test
    void beggeSakerMedSammeForelderrolleFeilerLukket() {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        lagre(
            sakUtenVedtak(søker, annenPart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
                søknadsperiode(termindato, termindato.plusDays(4), Konto.MØDREKVOTE)),
            sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
                søknadsperiode(termindato, termindato.plusDays(4), Konto.MØDREKVOTE)));

        assertThatThrownBy(() -> tjeneste(søker).hentFor(søker, annenPart, barn, termindato))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Ugyldig rolle i uttaksplan");
    }

    @Test
    void overlappendePerioderFraValgtSøknadAvvisesAvTidslinjen() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var sak = sakUtenVedtak(søker, null, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE));
        lagre(sak);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, List.of(
                nyPeriode(Søknad.Rolle.MOR, mandag, mandag.plusDays(4), SøknadPeriode.KontoType.MØDREKVOTE, null, null, null),
                nyPeriode(Søknad.Rolle.MOR, mandag.plusDays(2), mandag.plusDays(6), SøknadPeriode.KontoType.FELLESPERIODE, null, null, null))));

        assertThatThrownBy(() -> tjeneste(søker, repository).hentFor(søker, null, barn, mandag))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Overlapp ikke tillat uten overlapCombinator");
    }

    @Test
    void aktivSøknadGirSammePlanUavhengigAvPerioderekkefølge() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var mandag = LocalDate.of(2026, 10, 5);
        var sak = sakUtenVedtak(søker, null, barn, mandag, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR,
            søknadsperiode(mandag, mandag.plusDays(4), Konto.MØDREKVOTE));
        lagre(sak);
        var først = nyPeriode(Søknad.Rolle.MOR, mandag.plusWeeks(3), mandag.plusWeeks(3).plusDays(4),
            SøknadPeriode.KontoType.FELLESPERIODE, null, null, null);
        var sist = nyPeriode(Søknad.Rolle.MOR, mandag, mandag.plusDays(4), SøknadPeriode.KontoType.MØDREKVOTE, null, null, null);
        var repository = new SøknadRepositoryStub();
        repository.lagre(sak.saksnummer(), new Søknad(Instant.parse("2026-09-01T12:00:00Z"),
            Søknad.Rolle.MOR, List.of(først, sist)));

        var plan = tjeneste(søker, repository).hentFor(søker, null, barn, mandag).orElseThrow();

        assertThat(plan.perioder()).extracting(p -> p.fom()).containsExactly(mandag, mandag.plusWeeks(3));
    }

    private UttaksplanTjeneste tjeneste(AktørId søker) {
        return tjeneste(søker, new SøknadRepositoryStub());
    }

    private UttaksplanTjeneste tjeneste(AktørId søker, SøknadRepository søknadRepository) {
        return tjeneste(søker, søknadRepository, sakRepository);
    }

    private UttaksplanTjeneste tjeneste(AktørId søker, SøknadRepository søknadRepository, RepositoryStub repository) {
        var saker = new Saker(repository, myndigInnloggetBruker(søker), annenpartUbeskyttetAdresse());
        return new UttaksplanTjeneste(saker, new AnnenPartSakTjeneste(saker), søknadRepository);
    }

    private void lagre(SakFP0... saker) {
        for (var sak : saker) {
            sakRepository.lagre(sak);
        }
    }

    private static SakFP0 sakUtenVedtak(AktørId aktørId,
                                        AktørId annenPart,
                                        AktørId barn,
                                        LocalDate termindato,
                                        int antallBarn,
                                        Dekningsgrad dekningsgrad,
                                        BrukerRolle rolle,
                                        FpSøknadsperiode periode) {
        var søknad = new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(periode), dekningsgrad, false);
        return sak(aktørId, annenPart, barn, termindato, antallBarn, rolle, Set.of(), Set.of(søknad));
    }

    private static SakFP0 sakMedVedtak(AktørId aktørId,
                                       AktørId annenPart,
                                       AktørId barn,
                                       LocalDate termindato,
                                       int antallBarn,
                                       Dekningsgrad dekningsgrad,
                                       BrukerRolle rolle,
                                       Uttaksperiode periode) {
        var vedtak = new FpVedtak(LocalDateTime.now(), List.of(periode), dekningsgrad, null, null, null);
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, LocalDateTime.now().minusDays(1), Set.of(), dekningsgrad, false);
        return sak(aktørId, annenPart, barn, termindato, antallBarn, rolle, Set.of(vedtak), Set.of(søknad));
    }

    private static SakFP0 sak(AktørId aktørId,
                              AktørId annenPart,
                              AktørId barn,
                              LocalDate termindato,
                              int antallBarn,
                              BrukerRolle rolle,
                              Set<FpVedtak> vedtak,
                              Set<FpSøknad> søknader) {
        return new SakFP0(Saksnummer.dummy(), aktørId, false, vedtak, annenPart, new FamilieHendelse(null, termindato, antallBarn, null), Set.of(),
            søknader, rolle, Set.of(barn), new Rettigheter(false, false, false), false, LocalDateTime.now());
    }

    private static FpSøknadsperiode søknadsperiode(LocalDate fom, LocalDate tom, Konto konto) {
        return new FpSøknadsperiode(fom, tom, konto, null, null, null, null, null, false, null);
    }

    private static FpSøknadsperiode legacyUtsettelse(LocalDate fom, LocalDate tom) {
        return new FpSøknadsperiode(fom, tom, null, no.nav.foreldrepenger.oversikt.domene.fp.UtsettelseÅrsak.ARBEID,
            null, null, null, null, false, null);
    }

    private static SøknadPeriode nyPeriode(Søknad.Rolle rolle, LocalDate fom, LocalDate tom, SøknadPeriode.KontoType konto,
                                           java.math.BigDecimal gradering, SøknadPeriode.AktivitetType aktivitet, String arbeidsgiver) {
        return new SøknadPeriode(fom, tom, rolle, konto, null, null, null, gradering, null, false, aktivitet, arbeidsgiver,
            arbeidsgiver == null ? null : SøknadPeriode.ArbeidsgiverType.ORGANISASJON);
    }

    private static SøknadPeriode utsettelsePeriode(Søknad.Rolle rolle, LocalDate fom, LocalDate tom) {
        return new SøknadPeriode(fom, tom, rolle, null, SøknadPeriode.UtsettelseÅrsak.ARBEID, null, null,
            null, null, false, null, null, null);
    }

    private static Uttaksperiode uttaksperiode(LocalDate fom, LocalDate tom, Konto konto) {
        var aktivitet = new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.FRILANS, null, null), konto,
            new Trekkdager(5), Prosent.ZERO);
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktivitet), false);
        return new Uttaksperiode(fom, tom, null, null, null, Prosent.ZERO, false, null, resultat);
    }

    private static Uttaksperiode vedtaksUtsettelse(LocalDate fom, LocalDate tom) {
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET, Uttaksperiode.Resultat.Årsak.ANNET, Set.of(), false);
        return new Uttaksperiode(fom, tom, no.nav.foreldrepenger.oversikt.domene.fp.UtsettelseÅrsak.ARBEID,
            null, null, Prosent.ZERO, false, null, resultat);
    }
}
