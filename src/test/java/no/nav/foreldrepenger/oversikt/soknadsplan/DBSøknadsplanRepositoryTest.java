package no.nav.foreldrepenger.oversikt.soknadsplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.JpaExtension;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ExtendWith(JpaExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
class DBSøknadsplanRepositoryTest {

    @Test
    void lagrerOgHenterAlleEnumverdierProsentverdierOgPeriodeliste(EntityManager entityManager) {
        var saksnummer = Saksnummer.dummy();
        var perioder = alleEnumverdier();
        var søknad = new Søknadsplan(Instant.parse("2026-03-29T01:30:00.123456Z"), Søknadsplan.Rolle.FAR_MEDMOR, perioder);
        var repository = new DBSøknadsplanRepository(entityManager);

        repository.lagre(saksnummer, søknad);
        entityManager.clear();

        assertSøknadUtenPeriodelisteorden(repository.hentAktivFor(saksnummer), søknad);
        assertThat(antall(entityManager, "select count(*) from soknadsplan where saksnummer = ? and aktiv = true", saksnummer.value())).isEqualTo(1L);
        assertThat(antall(entityManager, "select count(*) from soknadsplan_periode p join soknadsplan s on s.id = p.soknadsplan_id where s.saksnummer = ?", saksnummer.value()))
            .isEqualTo((long) perioder.size());
    }

    @Test
    void erstatterAktivSøknadUtenÅEndreHistorikkOgUavhengigAvMottattTidspunkt(EntityManager entityManager) {
        var saksnummer = Saksnummer.dummy();
        var annenSak = Saksnummer.dummy();
        var repository = new DBSøknadsplanRepository(entityManager);
        var historisk = søknad(Instant.parse("2026-05-02T10:00:00Z"), List.of(periode(1, Boolean.TRUE)));
        var aktivPåAnnenSak = søknad(Instant.parse("2026-05-03T10:00:00Z"), List.of(periode(2, Boolean.FALSE)));
        var nyereFørst = søknad(Instant.parse("2026-05-10T10:00:00Z"), List.of(periode(3, null), periode(4, Boolean.TRUE)));
        var eldreSendtSenere = søknad(Instant.parse("2026-05-01T10:00:00Z"), List.of(periode(5, Boolean.FALSE)));

        repository.lagre(saksnummer, historisk);
        repository.lagre(annenSak, aktivPåAnnenSak);
        repository.lagre(saksnummer, nyereFørst);
        entityManager.clear();
        repository.lagre(saksnummer, eldreSendtSenere);
        entityManager.clear();

        assertSøknadUtenPeriodelisteorden(repository.hentAktivFor(saksnummer), eldreSendtSenere);
        assertSøknadUtenPeriodelisteorden(repository.hentAktivFor(annenSak), aktivPåAnnenSak);
        assertThat(antall(entityManager, "select count(*) from soknadsplan where saksnummer = ?", saksnummer.value())).isEqualTo(3L);
        assertThat(antall(entityManager, "select count(*) from soknadsplan where saksnummer = ? and aktiv = true", saksnummer.value())).isEqualTo(1L);
        assertThat(antall(entityManager, "select count(*) from soknadsplan where saksnummer = ? and aktiv = false", saksnummer.value())).isEqualTo(2L);
        assertThat(antall(entityManager, "select count(*) from soknadsplan_periode p join soknadsplan s on s.id = p.soknadsplan_id where s.saksnummer = ?", saksnummer.value()))
            .isEqualTo(4L);
        assertThat(entityManager.createNativeQuery("select indexdef from pg_indexes where indexname = 'uidx_soknadsplan_aktiv'")
            .getSingleResult().toString().toLowerCase()).contains("unique index", "where (aktiv = true)");
    }

    @Test
    void lagrerOgRunderInstantGjennomPostgres(EntityManager entityManager) {
        var saksnummer = Saksnummer.dummy();
        var repository = new DBSøknadsplanRepository(entityManager);
        var dstOvergang = OffsetDateTime.of(2026, 3, 29, 3, 30, 0, 987654000, ZoneOffset.ofHours(2)).toInstant();
        var søknad = søknad(dstOvergang, List.of(periode(1, Boolean.TRUE)));

        repository.lagre(saksnummer, søknad);
        entityManager.clear();

        assertSøknadUtenPeriodelisteorden(repository.hentAktivFor(saksnummer), søknad);
        assertThat(repository.hentAktivFor(saksnummer).orElseThrow().mottattTidspunkt()).isEqualTo(dstOvergang);
        assertThat(antall(entityManager, "select count(*) from soknadsplan_periode p join soknadsplan s on s.id = p.soknadsplan_id where s.saksnummer = ?", saksnummer.value()))
            .isEqualTo(1L);
    }

    @Test
    void runderBigDecimalMedUbegrensetPresisjonGjennomPostgres(EntityManager entityManager) {
        var saksnummer = Saksnummer.dummy();
        var repository = new DBSøknadsplanRepository(entityManager);
        var gradering = new BigDecimal("37.12345678901234567890123456789012345678901234567890123456789012345678901234567890");
        var samtidigUttaksprosent = new BigDecimal("62.000000000000000000000000000000000000000000000000012345678901234567890123456789");
        var periode = new SøknadsplanPeriode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2), Søknadsplan.Rolle.MOR,
            SøknadsplanPeriode.KontoType.MØDREKVOTE, null, null, null, gradering, samtidigUttaksprosent, null, null, null);
        var søknad = søknad(Instant.parse("2026-03-01T10:00:00Z"), List.of(periode));

        repository.lagre(saksnummer, søknad);
        entityManager.clear();

        assertSøknadUtenPeriodelisteorden(repository.hentAktivFor(saksnummer), søknad);
    }

    @Test
    void lagrerOgHenterAktivitetOgArbeidsgiverIdForBeggeRoller(EntityManager entityManager) {
        var saksnummer = Saksnummer.dummy();
        var repository = new DBSøknadsplanRepository(entityManager);
        var arbeidsgiverId = "012345678";
        var perioder = List.of(
            periodeMedArbeidsgiver(Søknadsplan.Rolle.MOR, SøknadsplanPeriode.AktivitetType.FRILANS, arbeidsgiverId),
            periodeMedArbeidsgiver(Søknadsplan.Rolle.FAR_MEDMOR, SøknadsplanPeriode.AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE,
                "00123456789"),
            periodeMedArbeidsgiver(Søknadsplan.Rolle.MOR, null, null),
            periodeMedArbeidsgiver(Søknadsplan.Rolle.FAR_MEDMOR, SøknadsplanPeriode.AktivitetType.ANNET, "987654321"));
        var søknad = søknad(Instant.parse("2026-03-01T10:00:00Z"), perioder);

        repository.lagre(saksnummer, søknad);
        entityManager.clear();

        var gjenhentet = repository.hentAktivFor(saksnummer).orElseThrow();
        assertThat(gjenhentet.perioder()).containsExactlyInAnyOrderElementsOf(perioder);
        assertThat(perioder.get(0).toString()).doesNotContain(arbeidsgiverId);
    }

    private static void assertSøknadUtenPeriodelisteorden(java.util.Optional<Søknadsplan> faktisk, Søknadsplan forventet) {
        assertThat(faktisk).get().satisfies(gjenhentet -> {
            assertThat(gjenhentet.mottattTidspunkt()).isEqualTo(forventet.mottattTidspunkt());
            assertThat(gjenhentet.rolle()).isEqualTo(forventet.rolle());
            assertThat(gjenhentet.perioder()).containsExactlyInAnyOrderElementsOf(forventet.perioder());
        });
    }

    private static long antall(EntityManager entityManager, String sql, String saksnummer) {
        return ((Number) entityManager.createNativeQuery(sql).setParameter(1, saksnummer).getSingleResult()).longValue();
    }

    private static Søknadsplan søknad(Instant tidspunkt, List<SøknadsplanPeriode> perioder) {
        return new Søknadsplan(tidspunkt, Søknadsplan.Rolle.MOR, perioder);
    }

    private static SøknadsplanPeriode periode(int dag, Boolean flerbarnsdager) {
        return new SøknadsplanPeriode(LocalDate.of(2026, 1, dag), LocalDate.of(2026, 1, dag + 1), Søknadsplan.Rolle.MOR,
            SøknadsplanPeriode.KontoType.MØDREKVOTE, SøknadsplanPeriode.UtsettelseÅrsak.ARBEID, SøknadsplanPeriode.OverføringÅrsak.ALENEOMSORG,
            SøknadsplanPeriode.MorsAktivitet.UTDANNING, new BigDecimal("37.50"), new BigDecimal("62.25"), flerbarnsdager, null, null);
    }

    private static SøknadsplanPeriode periodeMedArbeidsgiver(Søknadsplan.Rolle rolle, SøknadsplanPeriode.AktivitetType aktivitetType,
                                                          String arbeidsgiverId) {
        return new SøknadsplanPeriode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), rolle,
            SøknadsplanPeriode.KontoType.FORELDREPENGER, null, null, null, null, null, null,
            aktivitetType, arbeidsgiverId);
    }

    private static List<SøknadsplanPeriode> alleEnumverdier() {
        var count = Math.max(SøknadsplanPeriode.KontoType.values().length,
            Math.max(SøknadsplanPeriode.UtsettelseÅrsak.values().length,
                Math.max(SøknadsplanPeriode.OverføringÅrsak.values().length, SøknadsplanPeriode.MorsAktivitet.values().length)));
        var perioder = new ArrayList<SøknadsplanPeriode>();
        var prosenter = List.of(BigDecimal.ZERO, new BigDecimal("37.50"), new BigDecimal("100.00"));
        var flerbarn = new Boolean[] {null, Boolean.FALSE, Boolean.TRUE};
        for (int i = 0; i < count; i++) {
            perioder.add(new SøknadsplanPeriode(LocalDate.of(2026, 2, i + 1), LocalDate.of(2026, 2, i + 1),
                Søknadsplan.Rolle.values()[i % Søknadsplan.Rolle.values().length], SøknadsplanPeriode.KontoType.values()[i % SøknadsplanPeriode.KontoType.values().length],
                SøknadsplanPeriode.UtsettelseÅrsak.values()[i % SøknadsplanPeriode.UtsettelseÅrsak.values().length],
                SøknadsplanPeriode.OverføringÅrsak.values()[i % SøknadsplanPeriode.OverføringÅrsak.values().length],
                SøknadsplanPeriode.MorsAktivitet.values()[i % SøknadsplanPeriode.MorsAktivitet.values().length],
                prosenter.get(i % prosenter.size()), prosenter.get((i + 1) % prosenter.size()), flerbarn[i % flerbarn.length], null, null));
        }
        return List.copyOf(perioder);
    }
}
