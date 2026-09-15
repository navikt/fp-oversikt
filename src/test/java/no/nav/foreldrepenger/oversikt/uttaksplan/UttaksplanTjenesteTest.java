package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenpartUbeskyttetAdresse;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.JpaExtension;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.DBSakRepository;
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
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.Saker;

@ExtendWith(JpaExtension.class)
class UttaksplanTjenesteTest {

    @Test
    void skalBrukeSøkersSøknadsperioderMenIkkeAnnenPartsNårVedtakMangler(EntityManager entityManager) {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søkersPeriode = søknadsperiode(termindato.minusWeeks(3), termindato.minusDays(1), Konto.FORELDREPENGER_FØR_FØDSEL);
        var annenPartsPeriode = søknadsperiode(termindato.plusWeeks(6), termindato.plusWeeks(8), Konto.FEDREKVOTE);
        lagre(entityManager, sakUtenVedtak(søker, annenPart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR, søkersPeriode),
            sakUtenVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, annenPartsPeriode));

        var plan = tjeneste(entityManager, søker).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.fom()).isEqualTo(søkersPeriode.fom());
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.annenPart()).isNull();
        });
    }

    @Test
    void skalFjerneArbeidsgiverFraAnnenPartsGradering(EntityManager entityManager) {
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
        lagre(entityManager, sakMedVedtak(annenPart, søker, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, uttaksperiode));

        var plan = tjeneste(entityManager, søker).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.annenPart().gradering().aktivitet()).isNull();
        });
    }

    @Test
    void skalIkkeReturnereAnnenPartsPerioderNårAnnenPartIkkeHarOppgittSøker(EntityManager entityManager) {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var tredjePart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søkersPeriode = søknadsperiode(termindato.minusWeeks(3), termindato.minusDays(1), Konto.FORELDREPENGER_FØR_FØDSEL);
        var annenPartsPeriode = uttaksperiode(termindato.plusWeeks(6), termindato.plusWeeks(8), Konto.FEDREKVOTE);
        lagre(entityManager, sakUtenVedtak(søker, annenPart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR, søkersPeriode),
            sakMedVedtak(annenPart, tredjePart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR, annenPartsPeriode));

        var plan = tjeneste(entityManager, søker).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.annenPart()).isNull();
        });
    }

    @Test
    void skalGiSammeResponsForUkjentSakOgManglendeRelasjon(EntityManager entityManager) {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var tredjePart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var tjeneste = tjeneste(entityManager, søker);
        var ukjentSak = tjeneste.hentFor(søker, annenPart, barn, termindato);

        lagre(entityManager, sakUtenVedtak(annenPart, tredjePart, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
            søknadsperiode(termindato, termindato.plusWeeks(2), Konto.FEDREKVOTE)));
        var manglendeRelasjon = tjeneste.hentFor(søker, annenPart, barn, termindato);
        var skjermetAnnenPart = tjeneste.hentFor(søker, null, barn, termindato);

        assertThat(ukjentSak).isEmpty();
        assertThat(manglendeRelasjon).isEqualTo(ukjentSak);
        assertThat(skjermetAnnenPart).isEqualTo(ukjentSak);
    }

    @Test
    void skalUtledeMetadataFraSøkerensSakNårDenFinnes(EntityManager entityManager) {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var søkersTermindato = LocalDate.of(2026, 10, 1);
        var annenPartsTermindato = søkersTermindato.plusDays(2);
        lagre(entityManager, sakUtenVedtak(søker, annenPart, barn, søkersTermindato, 2, Dekningsgrad.ÅTTI, BrukerRolle.MOR,
                søknadsperiode(søkersTermindato, søkersTermindato.plusWeeks(2), Konto.MØDREKVOTE)),
            sakUtenVedtak(annenPart, søker, barn, annenPartsTermindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.FAR,
                søknadsperiode(annenPartsTermindato, annenPartsTermindato.plusWeeks(2), Konto.FEDREKVOTE)));

        var plan = tjeneste(entityManager, søker).hentFor(søker, annenPart, barn, søkersTermindato).orElseThrow();

        assertThat(plan.termindato()).isEqualTo(søkersTermindato);
        assertThat(plan.antallBarn()).isEqualTo(2);
        assertThat(plan.dekningsgrad()).isEqualTo(FellesUttaksplanDto.Dekningsgrad.ÅTTI);
    }

    @Test
    void skalUtledeMetadataFraAnnenPartsSakVedFørstegangssøknad(EntityManager entityManager) {
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        lagre(entityManager, sakUtenVedtak(annenPart, søker, barn, termindato, 2, Dekningsgrad.ÅTTI, BrukerRolle.FAR,
            søknadsperiode(termindato, termindato.plusWeeks(2), Konto.FEDREKVOTE)));

        var plan = tjeneste(entityManager, søker).hentFor(søker, annenPart, barn, termindato).orElseThrow();

        assertThat(plan.termindato()).isEqualTo(termindato);
        assertThat(plan.antallBarn()).isEqualTo(2);
        assertThat(plan.dekningsgrad()).isEqualTo(FellesUttaksplanDto.Dekningsgrad.ÅTTI);
        assertThat(plan.perioder()).isEmpty();
    }

    @Test
    void skalByggePlanUtenAnnenPartNårAnnenForelderMangler(EntityManager entityManager) {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søkersPeriode = søknadsperiode(termindato.minusWeeks(3), termindato.minusDays(1), Konto.FORELDREPENGER_FØR_FØDSEL);
        lagre(entityManager, sakUtenVedtak(søker, null, barn, termindato, 1, Dekningsgrad.HUNDRE, BrukerRolle.MOR, søkersPeriode));

        var plan = tjeneste(entityManager, søker).hentFor(søker, null, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).singleElement().satisfies(periode -> {
            assertThat(periode.søker()).isNotNull();
            assertThat(periode.annenPart()).isNull();
            assertThat(periode.annenPartEøs()).isNull();
        });
    }

    private static UttaksplanTjeneste tjeneste(EntityManager entityManager, AktørId søker) {
        var saker = new Saker(new DBSakRepository(entityManager), myndigInnloggetBruker(søker), annenpartUbeskyttetAdresse());
        return new UttaksplanTjeneste(saker, new AnnenPartSakTjeneste(saker));
    }

    private static void lagre(EntityManager entityManager, SakFP0... saker) {
        var repository = new DBSakRepository(entityManager);
        for (var sak : saker) {
            repository.lagre(sak);
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

    private static Uttaksperiode uttaksperiode(LocalDate fom, LocalDate tom, Konto konto) {
        var aktivitet = new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.FRILANS, null, null), konto,
            new Trekkdager(5), Prosent.ZERO);
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktivitet), false);
        return new Uttaksperiode(fom, tom, null, null, null, Prosent.ZERO, false, null, resultat);
    }
}
