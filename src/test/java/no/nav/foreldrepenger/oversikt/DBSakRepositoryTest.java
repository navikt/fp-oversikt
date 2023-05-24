package no.nav.foreldrepenger.oversikt;

import static java.time.LocalDateTime.now;
import static java.util.Set.of;
import static no.nav.foreldrepenger.oversikt.domene.BrukerRolle.FAR;
import static no.nav.foreldrepenger.oversikt.domene.BrukerRolle.MOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.DBSakRepository;
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.Gradering;
import no.nav.foreldrepenger.oversikt.domene.Konto;
import no.nav.foreldrepenger.oversikt.domene.MorsAktivitet;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakStatus;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.Trekkdager;
import no.nav.foreldrepenger.oversikt.domene.UtsettelseÅrsak;
import no.nav.foreldrepenger.oversikt.domene.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode.Resultat;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode.UttaksperiodeAktivitet;

@ExtendWith(JpaExtension.class)
class DBSakRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var aktørId = AktørId.dummy();
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2), UtsettelseÅrsak.NAV_TILTAK, null,
            null, Prosent.ZERO, false, MorsAktivitet.IKKE_OPPGITT,
            new Resultat(Resultat.Type.INNVILGET, Resultat.Årsak.ANNET, Set.of(uttaksperiodeAktivitet(new Trekkdager(20))), false)));
        var vedtak = new FpVedtak(now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, now(), of(new FpSøknadsperiode(LocalDate.now(), LocalDate.now(), Konto.FELLESPERIODE,
            UtsettelseÅrsak.SØKER_SYKDOM, null, null, new Gradering(new Prosent(3), new UttakAktivitet(UttakAktivitet.Type.FRILANS,
            Arbeidsgiver.dummy(), null)), new Prosent(44), true, MorsAktivitet.ARBEID)), Dekningsgrad.HUNDRE);
        var originalt = new SakFP0(Saksnummer.dummy(), aktørId, SakStatus.UNDER_BEHANDLING, of(vedtak), AktørId.dummy(), fh(), aksjonspunkt(),
            of(søknad), MOR, of(AktørId.dummy()), beggeRett(), false);
        repository.lagre(originalt);
        var annenAktørsSak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.AVSLUTTET, null, AktørId.dummy(), fh(), aksjonspunkt(), of(),
            MOR, of(AktørId.dummy()), beggeRett(), false);
        repository.lagre(annenAktørsSak);

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.get(0)).isEqualTo(originalt);
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
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2), null, null, null, Prosent.ZERO,
            false, null, new Resultat(Resultat.Type.AVSLÅTT, Resultat.Årsak.ANNET, Set.of(uttaksperiodeAktivitet(Trekkdager.ZERO)), false)));
        var vedtak = new FpVedtak(now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var saksnummer = Saksnummer.dummy();
        var annenPartAktørId = AktørId.dummy();
        var barn = of(AktørId.dummy());
        var originalt = new SakFP0(saksnummer, aktørId, SakStatus.UNDER_BEHANDLING, of(vedtak), annenPartAktørId, fh(), aksjonspunkt(), of(), FAR,
            barn, beggeRett(), false);
        repository.lagre(originalt);
        var oppdatertSak = new SakFP0(saksnummer, aktørId, SakStatus.UNDER_BEHANDLING, null, annenPartAktørId, fh(), aksjonspunkt(),
            of(new FpSøknad(SøknadStatus.MOTTATT, now(), null, Dekningsgrad.HUNDRE)), FAR, barn, beggeRett(), false);
        repository.lagre(oppdatertSak);

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.get(0)).isNotEqualTo(originalt);
        assertThat(saker.get(0)).isEqualTo(oppdatertSak);
    }
}
