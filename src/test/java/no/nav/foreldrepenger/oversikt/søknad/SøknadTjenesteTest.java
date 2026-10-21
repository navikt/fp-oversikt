package no.nav.foreldrepenger.oversikt.søknad;

import static no.nav.foreldrepenger.oversikt.søknad.Søknad.Rolle.MOR;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.MØDREKVOTE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPerioderTest.periode;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadType.ENDRINGSSØKNAD;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadType.FØRSTEGANGSSØKNAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.stub.SøknadRepositoryStub;

class SøknadTjenesteTest {

    private static final LocalDate DAG = LocalDate.of(2026, 11, 2);
    private static final Instant MOTTATT = Instant.parse("2026-10-01T10:00:00Z");

    private final SøknadRepositoryStub søknadRepository = new SøknadRepositoryStub();
    private final SøknadTjeneste tjeneste = new SøknadTjeneste(søknadRepository);

    @Test
    void førstegangssøknadErstatterAlleLagredePerioder() {
        var saksnummer = Saksnummer.dummy();
        søknadRepository.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(20), MØDREKVOTE, null)));
        var ny = søknad(MOTTATT.plusSeconds(60), periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null));

        tjeneste.lagre(saksnummer, ny, FØRSTEGANGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(ny);
    }

    @Test
    void førstegangssøknadLagresUtenSak() {
        var saksnummer = Saksnummer.dummy();
        var ny = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null));

        tjeneste.lagre(saksnummer, ny, FØRSTEGANGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(ny);
    }

    @Test
    void endringssøknadSlåsSammenMedLagretSøknad() {
        var saksnummer = Saksnummer.dummy();
        søknadRepository.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(20), MØDREKVOTE, null)));
        var endring = periode(MOR, DAG.plusDays(10), DAG.plusDays(12), FELLESPERIODE, null);

        tjeneste.lagre(saksnummer, søknad(MOTTATT.plusSeconds(60), endring), ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(saksnummer).orElseThrow().perioder())
            .containsExactly(periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null), endring);
    }

    @Test
    void endringssøknadUtenLagretSøknadLagresIkke() {
        var saksnummer = Saksnummer.dummy();

        tjeneste.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null)), ENDRINGSSØKNAD);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(SøknadType.class)
    void søknadSomIkkeErNyereEnnAktivIgnoreres(SøknadType søknadType) {
        var saksnummer = Saksnummer.dummy();
        var aktiv = søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null));
        søknadRepository.lagre(saksnummer, aktiv);

        tjeneste.lagre(saksnummer, søknad(MOTTATT, periode(MOR, DAG, DAG.plusDays(9), FELLESPERIODE, null)), søknadType);
        tjeneste.lagre(saksnummer, søknad(MOTTATT.minusSeconds(60), periode(MOR, DAG, DAG.plusDays(9), FELLESPERIODE, null)), søknadType);

        assertThat(søknadRepository.hentAktivFor(saksnummer)).contains(aktiv);
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
}
