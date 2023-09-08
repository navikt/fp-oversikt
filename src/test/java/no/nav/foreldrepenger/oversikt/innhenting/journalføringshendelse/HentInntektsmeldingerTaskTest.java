package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Beløp;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.Inntektsmelding;

class HentInntektsmeldingerTaskTest {

    @Test
    void map() {
        var fraFpsak = new Inntektsmelding("123", Arbeidsgiver.dummy(), LocalDateTime.now(), new Beløp(100));
        var resultat = HentInntektsmeldingerTask.map(fraFpsak);

        assertThat(resultat.innsendingstidspunkt()).isEqualTo(fraFpsak.innsendingstidspunkt());
        assertThat(resultat.journalpostId()).isEqualTo(fraFpsak.journalpostId());
        assertThat(resultat.inntekt()).isEqualTo(fraFpsak.inntekt());
        assertThat(resultat.arbeidsgiver()).isEqualTo(fraFpsak.arbeidsgiver());
    }
}
