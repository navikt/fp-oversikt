package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende.ManglendeVedleggRepository;
import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hent.manglendeVedlegg")
public class HentMangledeVedleggTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentMangledeVedleggTask.class);

    private final FpsakTjeneste fpsakTjeneste;
    private final ManglendeVedleggRepository manglendeVedleggRepository;

    @Inject
    public HentMangledeVedleggTask(FpsakTjeneste fpsakTjeneste, ManglendeVedleggRepository manglendeVedleggRepository) {
        this.fpsakTjeneste = fpsakTjeneste;
        this.manglendeVedleggRepository = manglendeVedleggRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        var manglendeVedlegg = fpsakTjeneste.hentMangelendeVedlegg(saksnummer);
        LOG.info("Hentet manglende vedlegg for sak {}: {}", saksnummer.value(), manglendeVedlegg);
        if (manglendeVedlegg.isEmpty()) {
            manglendeVedleggRepository.slett(saksnummer);
        } else {
            manglendeVedleggRepository.lagreManglendeVedleggPåSak(saksnummer, manglendeVedlegg);
        }
    }
}
