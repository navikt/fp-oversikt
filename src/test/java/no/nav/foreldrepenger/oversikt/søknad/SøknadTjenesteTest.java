package no.nav.foreldrepenger.oversikt.søknad;

import static no.nav.foreldrepenger.oversikt.søknad.Søknad.Rolle.MOR;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.MØDREKVOTE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPerioderTest.periode;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.fp.Trekkdager;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;
import no.nav.foreldrepenger.oversikt.stub.SøknadRepositoryStub;

class SøknadTjenesteTest {

    private static final LocalDate DAG = LocalDate.of(2026, 11, 2);
    private static final Instant MOTTATT = Instant.parse("2026-10-01T10:00:00Z");

    private final SøknadRepositoryStub søknadRepository = new SøknadRepositoryStub();
    private final RepositoryStub sakRepository = new RepositoryStub();
    private final SøknadTjeneste tjeneste = new SøknadTjeneste(søknadRepository, sakRepository);

    @Test
    void førstegangssøknadErstatterAlleLagredePerioder() {
        var saksnummer = lagreSak(false);
        søknadRepository.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(20), MØDREKVOTE, null)));
        var ny = søknad(MOTTATT.plusSeconds(60), periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null));

        tjeneste.lagre(saksnummer, ny);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(ny);
    }

    @Test
    void sakSomManglerBehandlesSomFørstegangssøknad() {
        var saksnummer = Saksnummer.dummy();
        var ny = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null));

        tjeneste.lagre(saksnummer, ny);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(ny);
    }

    @Test
    void endringssøknadSlåsSammenMedLagretSøknad() {
        var saksnummer = lagreSak(true);
        søknadRepository.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(20), MØDREKVOTE, null)));
        var endring = periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null);

        tjeneste.lagre(saksnummer, søknad(MOTTATT.plusSeconds(60), endring));

        assertThat(søknadRepository.hentAktivFor(saksnummer).orElseThrow().perioder())
            .containsExactly(periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null), endring);
    }

    @Test
    void endringssøknadUtenLagretSøknadLagresIkke() {
        var saksnummer = lagreSak(true);

        tjeneste.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null)));

        assertThat(søknadRepository.hentAktivFor(saksnummer)).isEmpty();
    }

    @Test
    void søknadSomIkkeErNyereEnnAktivIgnoreres() {
        var saksnummer = lagreSak(false);
        var aktiv = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null));
        søknadRepository.lagre(saksnummer, aktiv);

        tjeneste.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(9), FELLESPERIODE, null)));
        tjeneste.lagre(saksnummer, søknad(MOTTATT.minusSeconds(60), periode(MOR, DAG, DAG.plusDays(9), FELLESPERIODE, null)));

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(aktiv);
    }

    private Saksnummer lagreSak(boolean kanSøkeOmEndring) {
        var vedtak = kanSøkeOmEndring ? Set.of(vedtakMedTrekkdager()) : Set.<FpVedtak>of();
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, vedtak, AktørId.dummy(),
            new FamilieHendelse(null, DAG, 1, null), Set.of(), Set.of(), BrukerRolle.MOR, Set.of(),
            new Rettigheter(false, false, false), false, LocalDateTime.now());
        assertThat(sak.kanSøkeOmEndring()).isEqualTo(kanSøkeOmEndring);
        sakRepository.lagre(sak);
        return sak.saksnummer();
    }

    private static FpVedtak vedtakMedTrekkdager() {
        var aktivitet = new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null),
            Konto.MØDREKVOTE, new Trekkdager(5), Prosent.ZERO);
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktivitet), false);
        var periode = new Uttaksperiode(DAG, DAG.plusDays(4), null, null, null, Prosent.ZERO, false, null, resultat);
        return new FpVedtak(LocalDateTime.now(), List.of(periode), Dekningsgrad.HUNDRE, null, null, null);
    }

    private static Søknad søknad(Instant mottatt, SøknadPeriode... perioder) {
        return new Søknad(mottatt, MOR, List.of(perioder));
    }
}
