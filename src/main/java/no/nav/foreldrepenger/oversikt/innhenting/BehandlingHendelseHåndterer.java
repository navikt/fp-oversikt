package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.hendelser.behandling.Hendelse;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandlingHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingHendelseHåndterer.class);

    private ProsessTaskTjeneste taskTjeneste;
    private FpsakTjeneste fpSakKlient;
    private SakRepository sakRepository;

    @Inject
    public BehandlingHendelseHåndterer(ProsessTaskTjeneste taskTjeneste, FpsakTjeneste fpSakKlient, SakRepository repository) {
        this.taskTjeneste = taskTjeneste;
        this.fpSakKlient = fpSakKlient;
        this.sakRepository = repository;
    }

    public BehandlingHendelseHåndterer() {

    }

    void handleMessage(String key, String payload) {
        LOG.info("Lest fra teamforeldrepenger.behandling-hendelse-v1: key={} payload={}", key, payload);
        try {
            var hendelse = map(payload);
            if (hendelse.getHendelse().equals(Hendelse.MIGRERING)) {
                hentSakMedEnGang(hendelse);
            } else {
                lagreHentSakTask(hendelse.getBehandlingUuid(), hendelse.getHendelseUuid(), hendelse.getSaksnummer());
            }
        } catch (Exception e) {
            LOG.warn("Feilet ved håndtering av hendelse. Ignorerer {}", key);
        }
    }

    private void hentSakMedEnGang(BehandlingHendelseV1 hendelse) {
        try {
            HentSakTask.hentOgLagreSak(fpSakKlient, sakRepository, hendelse.getBehandlingUuid());
        } catch (Exception e) {
            LOG.info("Direkte henting av sak feilet {}", hendelse.getSaksnummer(), e);
            //lager task for å prøve på nytt evt feile
            lagreHentSakTask(hendelse.getBehandlingUuid(), hendelse.getHendelseUuid(), hendelse.getSaksnummer());
        }
    }

    private void lagreHentSakTask(UUID behandlingUuid, UUID hendelseUuid, String saksnummer) {
        var task = opprettTask(behandlingUuid, hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
        LOG.info("Opprettet task");
    }

    public static ProsessTaskData opprettTask(UUID behandlingUuid, UUID hendelseUuid, String saksnummer) {
        var task = ProsessTaskData.forProsessTask(HentSakTask.class);
        task.setCallId(hendelseUuid.toString());
        task.setProperty(HentSakTask.BEHANDLING_UUID, behandlingUuid.toString());
        task.setPrioritet(50);
        task.medNesteKjøringEtter(LocalDateTime.now());
        task.setCallIdFraEksisterende();
        task.setGruppe(saksnummer);
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

    private static BehandlingHendelseV1 map(String payload) {
        return DefaultJsonMapper.fromJson(payload, BehandlingHendelseV1.class);
    }


}
