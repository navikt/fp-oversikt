package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.oversikt.Vedtak;
import no.nav.foreldrepenger.oversikt.VedtakRepository;
import no.nav.foreldrepenger.oversikt.VedtakV0;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hent.sak")
public class HentSakTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentSakTask.class);
    static final String BEHANDLING_UUID = "behandlingUuid";

    private final FpsakTjeneste fpSakKlient;
    private final VedtakRepository vedtakRepository;

    @Inject
    public HentSakTask(FpsakTjeneste fpsakTjeneste, VedtakRepository vedtakRepository) {
        this.fpSakKlient = fpsakTjeneste;
        this.vedtakRepository = vedtakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("kjører task");
        var behandlingUuid = UUID.fromString(prosessTaskData.getPropertyValue(BEHANDLING_UUID));
        var sakDto = fpSakKlient.hentSak(behandlingUuid);
        LOG.info("Hentet vedtak {} {}", behandlingUuid, sakDto);

        vedtakRepository.lagre(map(sakDto));
    }

    private Vedtak map(FpsakTjeneste.SakDto sakDto) {
        return new VedtakV0(sakDto.saksnummer(), sakDto.status().name(), sakDto.ytelseType().name(), sakDto.aktørId());
    }
}
