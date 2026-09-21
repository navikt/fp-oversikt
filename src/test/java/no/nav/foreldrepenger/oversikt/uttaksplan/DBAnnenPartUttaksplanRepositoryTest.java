package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.JpaExtension;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

@ExtendWith(JpaExtension.class)
class DBAnnenPartUttaksplanRepositoryTest {

    @Test
    void skalLagrePlan(EntityManager entityManager) {
        var repository = new DBAnnenPartUttaksplanRepository(entityManager);
        var saksnummer = Saksnummer.dummy();
        var plan = plan(LocalDateTime.of(2026, 9, 1, 12, 0), List.of(periode(LocalDate.of(2026, 10, 1))));

        repository.lagre(saksnummer, plan);

        assertThat(repository.hentFor(saksnummer)).contains(plan);
    }

    @Test
    void skalErstatteEksisterendePlan(EntityManager entityManager) {
        var repository = new DBAnnenPartUttaksplanRepository(entityManager);
        var saksnummer = Saksnummer.dummy();
        var første = plan(LocalDateTime.of(2026, 9, 1, 12, 0), List.of(periode(LocalDate.of(2026, 10, 1))));
        var erstattet = plan(LocalDateTime.of(2026, 9, 2, 12, 0), List.of(periode(LocalDate.of(2026, 11, 1))));
        repository.lagre(saksnummer, første);

        repository.lagre(saksnummer, erstattet);

        assertThat(repository.hentFor(saksnummer)).contains(erstattet);
    }

    @Test
    void skalLagreTomListeSomBevisstTømming(EntityManager entityManager) {
        var repository = new DBAnnenPartUttaksplanRepository(entityManager);
        var saksnummer = Saksnummer.dummy();
        var tømt = plan(LocalDateTime.of(2026, 9, 2, 12, 0), List.of());

        repository.lagre(saksnummer, tømt);

        assertThat(repository.hentFor(saksnummer)).contains(tømt);
    }

    @Test
    void skalIkkeOverskriveNyerePlanVedForsinketRetry(EntityManager entityManager) {
        var repository = new DBAnnenPartUttaksplanRepository(entityManager);
        var saksnummer = Saksnummer.dummy();
        var nyere = plan(LocalDateTime.of(2026, 9, 2, 12, 0), List.of(periode(LocalDate.of(2026, 11, 1))));
        var eldre = plan(LocalDateTime.of(2026, 9, 1, 12, 0), List.of(periode(LocalDate.of(2026, 10, 1))));
        repository.lagre(saksnummer, nyere);

        repository.lagre(saksnummer, eldre);

        assertThat(repository.hentFor(saksnummer)).contains(nyere);
    }

    private static AnnenPartUttaksplan plan(LocalDateTime mottattTidspunkt, List<Planperiode> perioder) {
        return new AnnenPartUttaksplan(mottattTidspunkt, perioder);
    }

    private static Planperiode periode(LocalDate fom) {
        var uttak = new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, null, null, null, null, null, false, null);
        return new Planperiode(fom, fom.plusWeeks(1), uttak);
    }
}
