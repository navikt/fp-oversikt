package no.nav.foreldrepenger.oversikt.soknadsplan;

import static no.nav.foreldrepenger.oversikt.soknadsplan.Søknadsplan.Rolle.MOR;
import static no.nav.foreldrepenger.oversikt.soknadsplan.SøknadsplanPeriode.KontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.oversikt.soknadsplan.SøknadsplanPeriode.KontoType.MØDREKVOTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.stub.SøknadsplanRepositoryStub;

class SøknadsplanTjenesteTest {

    private static final LocalDate DAG = LocalDate.of(2026, 11, 2);
    private static final Instant MOTTATT = Instant.parse("2026-10-01T10:00:00Z");

    private final SøknadsplanRepositoryStub søknadRepository = new SøknadsplanRepositoryStub();
    private final SøknadsplanTjeneste tjeneste = new SøknadsplanTjeneste(søknadRepository);

    @Test
    void nySøknadErstatterAlleLagredePerioder() {
        var saksnummer = Saksnummer.dummy();
        søknadRepository.lagre(saksnummer, søknad(MOTTATT, periode(DAG, DAG.plusDays(20), MØDREKVOTE)));
        var ny = søknad(MOTTATT.plusSeconds(60), periode(DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE));

        tjeneste.lagre(saksnummer, ny);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(ny);
    }

    @Test
    void søknadLagresUtenTidligereSøknad() {
        var saksnummer = Saksnummer.dummy();
        var ny = søknad(MOTTATT, periode(DAG, DAG.plusDays(4), MØDREKVOTE));

        tjeneste.lagre(saksnummer, ny);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(ny);
    }

    @Test
    void søknadSomIkkeErNyereEnnAktivIgnoreres() {
        var saksnummer = Saksnummer.dummy();
        var aktiv = søknad(MOTTATT, periode(DAG, DAG.plusDays(4), MØDREKVOTE));
        søknadRepository.lagre(saksnummer, aktiv);

        tjeneste.lagre(saksnummer, søknad(MOTTATT, periode(DAG, DAG.plusDays(9), FELLESPERIODE)));
        tjeneste.lagre(saksnummer, søknad(MOTTATT.minusSeconds(60), periode(DAG, DAG.plusDays(9), FELLESPERIODE)));

        assertThat(søknadRepository.hentAktivFor(saksnummer)).containsSame(aktiv);
    }

    private static Søknadsplan søknad(Instant mottatt, SøknadsplanPeriode... perioder) {
        return new Søknadsplan(mottatt, MOR, List.of(perioder));
    }

    private static SøknadsplanPeriode periode(LocalDate fom, LocalDate tom, SøknadsplanPeriode.KontoType konto) {
        return new SøknadsplanPeriode(fom, tom, MOR, konto, null, null, null, (BigDecimal) null, null, false, null, null);
    }
}
