package no.nav.foreldrepenger.oversikt.søknad;

import static no.nav.foreldrepenger.oversikt.søknad.Søknad.Rolle.FAR_MEDMOR;
import static no.nav.foreldrepenger.oversikt.søknad.Søknad.Rolle.MOR;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.FEDREKVOTE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.oversikt.søknad.SøknadPeriode.KontoType.MØDREKVOTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class SøknadPerioderTest {

    private static final LocalDate DAG = LocalDate.of(2026, 11, 2);

    @Test
    void endringMidtIPeriodeKutterEksisterendeOgBeholderVerdiene() {
        var gradert = periode(MOR, DAG, DAG.plusDays(20), FELLESPERIODE, new BigDecimal("50"));
        var ny = periode(MOR, DAG.plusDays(10), DAG.plusDays(15), MØDREKVOTE, null);

        var resultat = SøknadPerioder.medEksisterendeFørEndringsdato(List.of(gradert), List.of(ny));

        assertThat(resultat).containsExactly(
            periode(MOR, DAG, DAG.plusDays(9), FELLESPERIODE, new BigDecimal("50")),
            ny);
    }

    @Test
    void endringPåPeriodegrenseBeholderTidligerePerioderUendret() {
        var første = periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null);
        var andre = periode(MOR, DAG.plusDays(5), DAG.plusDays(9), FELLESPERIODE, null);
        var ny = periode(MOR, DAG.plusDays(5), DAG.plusDays(7), MØDREKVOTE, null);

        assertThat(SøknadPerioder.medEksisterendeFørEndringsdato(List.of(første, andre), List.of(ny))).containsExactly(første, ny);
    }

    @Test
    void endringEtterSistePeriodeLeggerTil() {
        var gammel = periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null);
        var ny = periode(MOR, DAG.plusDays(20), DAG.plusDays(24), FELLESPERIODE, null);

        assertThat(SøknadPerioder.medEksisterendeFørEndringsdato(List.of(gammel), List.of(ny))).containsExactly(gammel, ny);
    }

    @Test
    void endringErstatterBeggeRollerFraFørsteFomUansettRolle() {
        var morFør = periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null);
        var farEtter = periode(FAR_MEDMOR, DAG.plusDays(10), DAG.plusDays(19), FEDREKVOTE, null);
        var morSenere = periode(MOR, DAG.plusDays(20), DAG.plusDays(29), FELLESPERIODE, null);
        var nyMor = periode(MOR, DAG.plusDays(5), DAG.plusDays(9), FELLESPERIODE, null);

        var resultat = SøknadPerioder.medEksisterendeFørEndringsdato(List.of(morFør, farEtter, morSenere), List.of(nyMor));

        assertThat(resultat).containsExactly(periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null), nyMor);
    }

    @Test
    void samtidigUttakForBeggeRollerBeholdesFørEndringsdato() {
        var mor = periode(MOR, DAG, DAG.plusDays(9), MØDREKVOTE, null);
        var far = periode(FAR_MEDMOR, DAG, DAG.plusDays(9), FEDREKVOTE, null);
        var nyFar = periode(FAR_MEDMOR, DAG.plusDays(5), DAG.plusDays(14), FEDREKVOTE, null);

        var resultat = SøknadPerioder.medEksisterendeFørEndringsdato(List.of(mor, far), List.of(nyFar));

        assertThat(resultat).containsExactly(
            periode(MOR, DAG, DAG.plusDays(4), MØDREKVOTE, null),
            periode(FAR_MEDMOR, DAG, DAG.plusDays(4), FEDREKVOTE, null),
            nyFar);
    }

    @Test
    void utsettelseSomKuttesBeholderÅrsak() {
        var utsettelse = new SøknadPeriode(DAG, DAG.plusDays(9), MOR, null, SøknadPeriode.UtsettelseÅrsak.ARBEID,
            null, null, null, null, false, null, null, null);
        var ny = periode(MOR, DAG.plusDays(3), DAG.plusDays(9), MØDREKVOTE, null);

        var resultat = SøknadPerioder.medEksisterendeFørEndringsdato(List.of(utsettelse), List.of(ny));

        assertThat(resultat.getFirst().tom()).isEqualTo(DAG.plusDays(2));
        assertThat(resultat.getFirst().utsettelseÅrsak()).isEqualTo(SøknadPeriode.UtsettelseÅrsak.ARBEID);
    }

    @Test
    void endringSomStarterMedHullBeholderGamlePerioderFørFørsteNyeFom() {
        // Kjent begrensning: endringsdato er første fom, så fri i starten av endringen kan ikke fjerne gamle perioder.
        var gammel = periode(MOR, DAG, DAG.plusDays(19), MØDREKVOTE, null);
        var ny = periode(MOR, DAG.plusDays(15), DAG.plusDays(19), FELLESPERIODE, null);

        assertThat(SøknadPerioder.medEksisterendeFørEndringsdato(List.of(gammel), List.of(ny)))
            .containsExactly(periode(MOR, DAG, DAG.plusDays(14), MØDREKVOTE, null), ny);
    }

    static SøknadPeriode periode(Søknad.Rolle rolle, LocalDate fom, LocalDate tom, SøknadPeriode.KontoType konto, BigDecimal gradering) {
        return new SøknadPeriode(fom, tom, rolle, konto, null, null, null, gradering, null, false, null, null, null);
    }
}
