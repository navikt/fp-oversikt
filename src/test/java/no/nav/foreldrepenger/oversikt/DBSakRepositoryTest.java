package no.nav.foreldrepenger.oversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.DBSakRepository;
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.Egenskap;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakStatus;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.Vedtak;

@ExtendWith(JpaExtension.class)
class DBSakRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var aktørId = AktørId.dummy();
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(
            Uttaksperiode.Resultat.Type.INNVILGET)));
        var vedtak = new Vedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var originalt = new SakFP0(Saksnummer.dummy(), aktørId, SakStatus.UNDER_BEHANDLING, Set.of(vedtak), AktørId.dummy(), fh(), aksjonspunkt(),
            egenskaper());
        repository.lagre(originalt);
        var annenAktørsSak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.AVSLUTTET, null, AktørId.dummy(), fh(), aksjonspunkt(),
            egenskaper());
        repository.lagre(annenAktørsSak);

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.get(0)).isEqualTo(originalt);
    }

    private Set<Aksjonspunkt> aksjonspunkt() {
        return Set.of(new Aksjonspunkt("1234", Aksjonspunkt.Status.UTFØRT, "VENTER", LocalDateTime.now()));
    }

    private static FamilieHendelse fh() {
        return new FamilieHendelse(LocalDate.now(), LocalDate.now(), 2, LocalDate.now());
    }

    @Test
    void oppdatererJsonPåSak(EntityManager entityManager) {
        var repository = new DBSakRepository(entityManager);
        var aktørId = AktørId.dummy();
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(
            Uttaksperiode.Resultat.Type.AVSLÅTT)));
        var vedtak = new Vedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var saksnummer = Saksnummer.dummy();
        var annenPartAktørId = AktørId.dummy();
        var originalt = new SakFP0(saksnummer, aktørId, SakStatus.UNDER_BEHANDLING, Set.of(vedtak), annenPartAktørId, fh(), aksjonspunkt(),
            egenskaper());
        repository.lagre(originalt);
        var oppdatertSak = new SakFP0(saksnummer, aktørId, SakStatus.UNDER_BEHANDLING, null, annenPartAktørId, fh(), aksjonspunkt(), egenskaper());
        repository.lagre(oppdatertSak);

        var saker = repository.hentFor(aktørId);

        assertThat(saker).hasSize(1);
        assertThat(saker.get(0)).isNotEqualTo(originalt);
        assertThat(saker.get(0)).isEqualTo(oppdatertSak);
    }

    private static Set<Egenskap> egenskaper() {
        return Set.of(Egenskap.SØKNAD_UNDER_BEHANDLING);
    }
}
