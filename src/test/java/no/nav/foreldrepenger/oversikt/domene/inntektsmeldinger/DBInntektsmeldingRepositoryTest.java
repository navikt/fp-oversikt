package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.oversikt.JpaExtension;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Beløp;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ExtendWith(JpaExtension.class)
class DBInntektsmeldingRepositoryTest {

    @Test
    void roundtrip(EntityManager entityManager) {
        var repository = new DBInntektsmeldingerRepository(entityManager);
        var før = new InntektsmeldingV1("123", Arbeidsgiver.dummy(), LocalDateTime.now(), Beløp.ZERO);
        var saksnummer = Saksnummer.dummy();
        repository.lagre(saksnummer, Set.of(før));

        var inntektsmeldinger = repository.hentFor(Set.of(saksnummer));
        assertThat(inntektsmeldinger).hasSize(1);

        var tilbakekreving = (InntektsmeldingV1) inntektsmeldinger.stream().findFirst().orElseThrow();
        assertThat(tilbakekreving.journalpostId()).isEqualTo(før.journalpostId());
        assertThat(tilbakekreving.inntekt()).isEqualTo(før.inntekt());
        assertThat(tilbakekreving.arbeidsgiver()).isEqualTo(før.arbeidsgiver());
        assertThat(tilbakekreving.innsendingstidspunkt()).isEqualTo(før.innsendingstidspunkt());

        repository.slett(saksnummer);
        assertThat(repository.hentFor(Set.of(saksnummer))).isEmpty();

    }

}
