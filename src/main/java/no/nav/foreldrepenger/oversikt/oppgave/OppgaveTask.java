package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.HashSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende.ManglendeVedleggRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "oppgave")
class OppgaveTask implements ProsessTaskHandler {

    private final OppgaveTjeneste oppgaveTjeneste;
    private final TilbakekrevingRepository tilbakekrevingRepository;
    private final ManglendeVedleggRepository manglendeVedleggRepository;

    @Inject
    OppgaveTask(OppgaveTjeneste oppgaveTjeneste,
                TilbakekrevingRepository tilbakekrevingRepository,
                ManglendeVedleggRepository manglendeVedleggRepository) {
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.manglendeVedleggRepository = manglendeVedleggRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());

        var oppgaver = new HashSet<OppgaveType>();
        var tilbakekreving = tilbakekrevingRepository.hentFor(saksnummer);
        tilbakekreving.flatMap(OppgaveUtleder::utledFor).ifPresent(oppgaver::add);

        var manglendeVedlegg = manglendeVedleggRepository.hentFor(saksnummer);
        OppgaveUtleder.utledFor(manglendeVedlegg).ifPresent(oppgaver::add);

        oppgaveTjeneste.oppdaterOppgaver(saksnummer, oppgaver);
    }


}
