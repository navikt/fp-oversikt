package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.HashSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende.ManglendeVedleggRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "opprett.oppgaver")
public class OpprettOppgaverTask implements ProsessTaskHandler {

    private static final Environment ENV = Environment.current();

    private final OppgaveTjeneste oppgaveTjeneste;
    private final ManglendeVedleggRepository manglendeVedleggRepository;

    @Inject
    OpprettOppgaverTask(OppgaveTjeneste oppgaveTjeneste,
                        ManglendeVedleggRepository manglendeVedleggRepository) {
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.manglendeVedleggRepository = manglendeVedleggRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());

        var oppgaver = new HashSet<OppgaveType>();

        var manglendeVedlegg = manglendeVedleggRepository.hentFor(saksnummer);
        OppgaveUtleder.utledFor(manglendeVedlegg).ifPresent(oppgaver::add);

        oppgaveTjeneste.oppdaterOppgaver(saksnummer, oppgaver);
    }


}
