package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentManglendeVedleggTask.TASK_TYPE;

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
@ProsessTask(TASK_TYPE)
public class HentManglendeVedleggTask implements ProsessTaskHandler {

    public static final String TASK_TYPE = "hent.manglendeVedlegg";
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

    public static void hentOgLagre(FpsakTjeneste fpsak, ManglendeVedleggRepository repository, Saksnummer saksnummer) {
        var manglendeVedlegg = fpsak.hentManglendeVedlegg(saksnummer);
        if (manglendeVedlegg.isEmpty()) {
            repository.slett(saksnummer);
        } else {
            LOG.info("Hentet {} manglende vedlegg for sak {}", manglendeVedlegg.size(), saksnummer.value());
            repository.lagreManglendeVedleggPåSak(saksnummer, manglendeVedlegg);
        }
    }
}
