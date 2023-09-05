package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask("hent.manglendeVedlegg")
public class HentMangledeVedleggTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentMangledeVedleggTask.class);

    private final FpsakTjeneste fpsakTjeneste;
    private final SakRepository sakRepository;

    @Inject
    public HentMangledeVedleggTask(FpsakTjeneste fpsakTjeneste, SakRepository sakRepository) {
        this.fpsakTjeneste = fpsakTjeneste;
        this.sakRepository = sakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = prosessTaskData.getSaksnummer();
        var manglendeVedlegg = fpsakTjeneste.hentMangelendeVedlegg(new Saksnummer(saksnummer));
        LOG.info("Hentet manglende vedlegg for sak {}: {}", saksnummer, manglendeVedlegg);
        sakRepository.lagreManglendeVedleggPåSak(saksnummer, manglendeVedlegg);
    }
}
