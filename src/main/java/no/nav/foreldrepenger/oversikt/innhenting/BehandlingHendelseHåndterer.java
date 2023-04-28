package no.nav.foreldrepenger.oversikt.innhenting;

import static no.nav.vedtak.hendelser.behandling.Hendelse.AVSLUTTET;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.hendelser.behandling.BehandlingHendelse;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandlingHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingHendelseHåndterer.class);

    private ProsessTaskTjeneste taskTjeneste;

    public BehandlingHendelseHåndterer() {
    }

    @Inject
    public BehandlingHendelseHåndterer(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;

    }

    void handleMessage(String key, String payload) {
        LOG.info("Lest fra teamforeldrepenger.behandling-hendelse-v1: key={} payload={}", key, payload);
        var hendelse = map(payload);
        if (Objects.equals(hendelse.getHendelse(), AVSLUTTET)) {
            opprettHentVedtakTask(hendelse);
        }
    }

    private void opprettHentVedtakTask(BehandlingHendelse hendelse) {
        var task = ProsessTaskData.forProsessTask(HentSakTask.class);
        task.setCallId(hendelse.getHendelseUuid().toString());
        task.setProperty(HentSakTask.BEHANDLING_UUID, hendelse.getBehandlingUuid().toString());
        task.setPrioritet(50);
        task.medNesteKjøringEtter(LocalDateTime.now());
        task.setCallIdFraEksisterende();
        taskTjeneste.lagre(task);
        LOG.info("Opprettet task");
    }

    private static BehandlingHendelse map(String payload) {
        return DefaultJsonMapper.fromJson(payload, BehandlingHendelse.class);
    }


}
