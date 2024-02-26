package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;

final class OppgaveUtleder {

    private OppgaveUtleder() {
    }

    public static Optional<OppgaveType> utledFor(List<DokumentType> manglendeVedlegg) {
        return manglendeVedlegg.isEmpty() ? Optional.empty() : Optional.of(OppgaveType.LAST_OPP_MANGLENDE_VEDLEGG);
    }
}
