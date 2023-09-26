package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.HashSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende.ManglendeVedleggRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "opprett.oppgaver")
public class OpprettOppgaverTask implements ProsessTaskHandler {

    private static final Environment ENV = Environment.current();

    private final OppgaveTjeneste oppgaveTjeneste;
    private final TilbakekrevingRepository tilbakekrevingRepository;
    private final ManglendeVedleggRepository manglendeVedleggRepository;

    @Inject
    OpprettOppgaverTask(OppgaveTjeneste oppgaveTjeneste,
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

        if (!ENV.isProd()) {
            var tilbakekreving = tilbakekrevingRepository.hentFor(saksnummer);
            tilbakekreving.flatMap(OppgaveUtleder::utledFor).ifPresent(oppgaver::add);
        }

        var manglendeVedlegg = manglendeVedleggRepository.hentFor(saksnummer);
        OppgaveUtleder.utledFor(manglendeVedlegg).ifPresent(oppgaver::add);

        oppgaveTjeneste.oppdaterOppgaver(saksnummer, oppgaver);
    }


}
