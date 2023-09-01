package no.nav.foreldrepenger.oversikt.innhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.hendelser.behandling.Hendelse;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandlingHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingHendelseHåndterer.class);

    private static final Set<Hendelse> IGNORE = Set.of(Hendelse.ENHET);

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

    void handleMessage(String topic, String key, String payload) {
        LOG.info("Lest fra : topic={}", topic);
        try {
            var hendelse = map(payload);
            if (hendelse.getHendelse().equals(Hendelse.MIGRERING)) {
                hentSakMedEnGang(hendelse);
            } else if (!IGNORE.contains(hendelse.getHendelse())) {
                lagreHentSakTask(hendelse.getHendelseUuid(), new Saksnummer(hendelse.getSaksnummer()));
            }
        } catch (Exception e) {
            LOG.warn("Feilet ved håndtering av hendelse. Ignorerer {}", key, e);
        }
    }

    private void hentSakMedEnGang(BehandlingHendelseV1 hendelse) {
        var saksnummer = new Saksnummer(hendelse.getSaksnummer());
        try {
            HentSakTask.hentOgLagreSak(fpSakKlient, sakRepository, saksnummer);
        } catch (Exception e) {
            LOG.info("Direkte henting av sak feilet {}", saksnummer.value(), e);
            //lager task for å prøve på nytt evt feile
            lagreHentSakTask(hendelse.getHendelseUuid(), saksnummer);
        }
    }

    private void lagreHentSakTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = opprettTask(hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
    }

    public static ProsessTaskData opprettTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = ProsessTaskData.forProsessTask(HentSakTask.class);
        task.setCallId(hendelseUuid.toString());
        task.setProperty(HentSakTask.SAKSNUMMER, saksnummer.value());
        task.setPrioritet(50);
        task.medNesteKjøringEtter(LocalDateTime.now());
        task.setGruppe(saksnummer.value());
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

    private static BehandlingHendelseV1 map(String payload) {
        return DefaultJsonMapper.fromJson(payload, BehandlingHendelseV1.class);
    }


}
