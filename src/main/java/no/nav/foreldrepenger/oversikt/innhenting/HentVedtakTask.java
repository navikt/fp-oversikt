package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.oversikt.Vedtak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.oversikt.VedtakRepository;
import no.nav.foreldrepenger.oversikt.VedtakV0;
import no.nav.foreldrepenger.oversikt.VedtakV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hent.vedtak")
public class HentVedtakTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentVedtakTask.class);
    static final String BEHANDLING_UUID = "behandlingUuid";

    private final FpSakKlient fpSakKlient;
    private final VedtakRepository vedtakRepository;

    @Inject
    public HentVedtakTask(FpSakKlient fpSakKlient, VedtakRepository vedtakRepository) {
        this.fpSakKlient = fpSakKlient;
        this.vedtakRepository = vedtakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("kjører task");
        var behandlingUuid = UUID.fromString(prosessTaskData.getPropertyValue(BEHANDLING_UUID));
        var vedtakDto = fpSakKlient.hentVedtak(behandlingUuid);
        LOG.info("Hentet vedtak {} {}", behandlingUuid, vedtakDto);

        vedtakRepository.lagre(map(vedtakDto));
    }

    private Vedtak map(FpSakKlient.VedtakDto vedtakDto) {
        if (Math.random() > 0.5) {
            return new VedtakV1(vedtakDto.opprettet(), vedtakDto.behandlendeEnhetNavn());
        }
        return new VedtakV0(vedtakDto.opprettet(), vedtakDto.uuid(), vedtakDto.behandlendeEnhetNavn());
    }
}
