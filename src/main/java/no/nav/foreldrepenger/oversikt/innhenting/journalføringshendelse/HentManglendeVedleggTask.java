package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende.ManglendeVedleggRepository;
import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.foreldrepenger.oversikt.oppgave.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hent.manglendeVedlegg")
public class HentManglendeVedleggTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentManglendeVedleggTask.class);

    private final FpsakTjeneste fpsakTjeneste;
    private final ManglendeVedleggRepository manglendeVedleggRepository;
    private final OppgaveTjeneste oppgaveTjeneste;

    @Inject
    public HentManglendeVedleggTask(FpsakTjeneste fpsakTjeneste,
                                    ManglendeVedleggRepository manglendeVedleggRepository,
                                    OppgaveTjeneste oppgaveTjeneste) {
        this.fpsakTjeneste = fpsakTjeneste;
        this.manglendeVedleggRepository = manglendeVedleggRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        hentOgLagre(fpsakTjeneste, manglendeVedleggRepository, saksnummer);
        oppgaveTjeneste.opprettOppdaterOppgaveTask(saksnummer);
    }

    public static void hentOgLagre(FpsakTjeneste fpsakTjeneste1, ManglendeVedleggRepository repository, Saksnummer saksnummer) {
        var manglendeVedlegg = fpsakTjeneste1.hentMangelendeVedlegg(saksnummer);
        LOG.info("Hentet manglende vedlegg for sak {}: {}", saksnummer.value(), manglendeVedlegg);
        if (manglendeVedlegg.isEmpty()) {
            repository.slett(saksnummer);
        } else {
            repository.lagreManglendeVedleggPåSak(saksnummer, manglendeVedlegg);
        }
    }
}
