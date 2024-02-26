package no.nav.foreldrepenger.oversikt.oppgave;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;


class OppgaveUtlederTest {

    @Test
    void oppgave_hvis_manglende_vedlegg() {
        var utledet = OppgaveUtleder.utledFor(List.of(DokumentType.I000061));
        assertThat(utledet).contains(OppgaveType.LAST_OPP_MANGLENDE_VEDLEGG);
    }

    @Test
    void ingen_oppgave_hvis_ingen_manglende_vedlegg() {
        var utledet = OppgaveUtleder.utledFor(List.of());
        assertThat(utledet).isEmpty();
    }
}
