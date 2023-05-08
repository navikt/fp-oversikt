package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Inject
    public BehandlingHendelseHåndterer(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;

    }

    public BehandlingHendelseHåndterer() {
    }

    void handleMessage(String key, String payload) {
        LOG.info("Lest fra teamforeldrepenger.behandling-hendelse-v1: key={} payload={}", key, payload);
        var hendelse = map(payload);
        lagreHentSakTask(hendelse.getBehandlingUuid(), hendelse.getHendelseUuid());
    }

    private void lagreHentSakTask(UUID behandlingUuid, UUID hendelseUuid) {
        var task = opprettTask(behandlingUuid, hendelseUuid);
        taskTjeneste.lagre(task);
        LOG.info("Opprettet task");
    }

    public static ProsessTaskData opprettTask(UUID behandlingUuid, UUID hendelseUuid) {
        var task = ProsessTaskData.forProsessTask(HentSakTask.class);
        task.setCallId(hendelseUuid.toString());
        task.setProperty(HentSakTask.BEHANDLING_UUID, behandlingUuid.toString());
        task.setPrioritet(50);
        task.medNesteKjøringEtter(LocalDateTime.now());
        task.setCallIdFraEksisterende();
        task.setGruppe(behandlingUuid.toString());
        return task;
    }

    private static BehandlingHendelse map(String payload) {
        return DefaultJsonMapper.fromJson(payload, BehandlingHendelse.class);
    }


}
