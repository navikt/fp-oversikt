package no.nav.foreldrepenger.oversikt;

import static java.time.LocalDateTime.now;
import static java.util.Set.of;
import static no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle.FAR;
import static no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle.MOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.DBSakRepository;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Gradering;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.MorsAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.fp.Trekkdager;
import no.nav.foreldrepenger.oversikt.domene.fp.UtsettelseÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.Resultat;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.UttaksperiodeAktivitet;

@ExtendWith(JpaExtension.class)
class DBSakRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var aktørId = AktørId.dummy();
        var uttaksperioder = List.of(
            new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2), UtsettelseÅrsak.NAV_TILTAK, null, null, Prosent.ZERO, false,
                MorsAktivitet.IKKE_OPPGITT,
                new Resultat(Resultat.Type.INNVILGET, Resultat.Årsak.ANNET, Set.of(uttaksperiodeAktivitet(new Trekkdager(20))), false)));
        var vedtak = new FpVedtak(now(), uttaksperioder, Dekningsgrad.HUNDRE, null, null, null);
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, now(),
            of(new FpSøknadsperiode(LocalDate.now(), LocalDate.now(), Konto.FELLESPERIODE, UtsettelseÅrsak.SØKER_SYKDOM, null, null,
                new Gradering(new Prosent(3), new UttakAktivitet(UttakAktivitet.Type.FRILANS, Arbeidsgiver.dummy(), null)), new Prosent(44), true,
                MorsAktivitet.ARBEID)), Dekningsgrad.HUNDRE, true);
        var originalt = new SakFP0(Saksnummer.dummy(), aktørId, false, of(vedtak), AktørId.dummy(), fh(), aksjonspunkt(), of(søknad), MOR,
            of(AktørId.dummy()), beggeRett(), false, LocalDateTime.now());
        repository.lagre(originalt);
        var annenAktørsSak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), true, null, AktørId.dummy(), fh(), aksjonspunkt(), of(), MOR,
            of(AktørId.dummy()), beggeRett(), false, LocalDateTime.now());
        repository.lagre(annenAktørsSak);

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.getFirst()).isEqualTo(originalt);
    }

    private static UttaksperiodeAktivitet uttaksperiodeAktivitet(Trekkdager trekkdager) {
        return new UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null), Konto.MØDREKVOTE,
            trekkdager, Prosent.ZERO);
    }

    private static Rettigheter beggeRett() {
        return new Rettigheter(false, false, false);
    }

    private Set<Aksjonspunkt> aksjonspunkt() {
        return of(new Aksjonspunkt(Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD, Aksjonspunkt.Venteårsak.SISTE_AAP_ELLER_DP_MELDEKORT, now()));
    }

    private static FamilieHendelse fh() {
        return new FamilieHendelse(LocalDate.now(), LocalDate.now(), 2, LocalDate.now());
    }

    @Test
    void oppdatererJsonPåSak(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var aktørId = AktørId.dummy();
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2), null, null, null, Prosent.ZERO, false, null,
            new Resultat(Resultat.Type.AVSLÅTT, Resultat.Årsak.ANNET, Set.of(uttaksperiodeAktivitet(Trekkdager.ZERO)), false)));
        var vedtak = new FpVedtak(now(), uttaksperioder, Dekningsgrad.HUNDRE, null, null,  null);
        var saksnummer = Saksnummer.dummy();
        var annenPartAktørId = AktørId.dummy();
        var barn = of(AktørId.dummy());
        var originalt = new SakFP0(saksnummer, aktørId, false, of(vedtak), annenPartAktørId, fh(), aksjonspunkt(), of(), FAR, barn, beggeRett(),
            false, LocalDateTime.now());
        repository.lagre(originalt);
        var oppdatertSak = new SakFP0(saksnummer, aktørId, false, null, annenPartAktørId, fh(), aksjonspunkt(),
            of(new FpSøknad(SøknadStatus.MOTTATT, now(), null, Dekningsgrad.HUNDRE, true)), FAR, barn, beggeRett(), false, LocalDateTime.now());
        repository.lagre(oppdatertSak);

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.getFirst()).isNotEqualTo(originalt);
        assertThat(saker.getFirst()).isEqualTo(oppdatertSak);
    }


    @Test
    void erSakKobletTilAktørReturnerTrueVedFlereSakerOgBareEnMatcher(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var saksnummer1 = Saksnummer.dummy();
        var saksnummer2 = Saksnummer.dummy();
        var aktørId = AktørId.dummy();

        var sak1 = new SakFP0(saksnummer1, aktørId, false, null, null, null, null, null, null, null, null, false, null);
        var sak2 = new SakFP0(saksnummer2, aktørId, false, null, null, null, null, null, null, null, null, false, null);

        repository.lagre(sak1);
        repository.lagre(sak2);

        assertThat(repository.erSakKobletTilAktør(saksnummer1, aktørId)).isTrue();
    }


    @Test
    void erSakKobletTilAktørReturnereFalseHvisSaksnummerIkkeMatcherNoenSakerPåAktørId(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var saksnummer1 = Saksnummer.dummy();
        var aktørId = AktørId.dummy();

        var sak1 = new SakFP0(saksnummer1, aktørId, false, null, null, null, null, null, null, null, null, false, null);

        repository.lagre(sak1);

        var saksnummerSomIkkeEksistererPåBruker = Saksnummer.dummy();
        assertThat(repository.erSakKobletTilAktør(saksnummerSomIkkeEksistererPåBruker, aktørId)).isFalse();
    }
}
