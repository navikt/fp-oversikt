package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.Tilbakekreving;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;

final class OppgaveUtleder {

    private OppgaveUtleder() {
    }

    static Optional<OppgaveType> utledFor(Tilbakekreving tilbakekreving) {
        return tilbakekreving.trengerSvarFraBruker() ? Optional.of(OppgaveType.SVAR_TILBAKEKREVING) : Optional.empty();

    }

    public static Optional<OppgaveType> utledFor(List<DokumentTypeId> manglendeVedlegg) {
        return !manglendeVedlegg.isEmpty() ? Optional.of(OppgaveType.LAST_OPP_MANGLENDE_VEDLEGG) : Optional.empty();
    }
}
