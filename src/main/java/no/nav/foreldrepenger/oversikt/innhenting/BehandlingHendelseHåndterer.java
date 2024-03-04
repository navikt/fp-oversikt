package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentInntektsmeldingerTask;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentManglendeVedleggTask;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentTilbakekrevingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.hendelser.behandling.Hendelse;
import no.nav.vedtak.hendelser.behandling.Kildesystem;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandlingHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingHendelseHåndterer.class);

    private static final Set<Hendelse> IGNORE = Set.of(Hendelse.ENHET, Hendelse.MIGRERING);

    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public BehandlingHendelseHåndterer(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    public BehandlingHendelseHåndterer() {

    }

    void handleMessage(String topic, String key, String payload) {
        LOG.debug("Lest fra : topic={}", topic);
        try {
            var hendelse = map(payload);
            var hendelseType = hendelse.getHendelse();
            if (!IGNORE.contains(hendelseType)) {
                var hendelseUuid = hendelse.getHendelseUuid();
                var saksnummer = new Saksnummer(hendelse.getSaksnummer());
                lagreHentSakTask(hendelseUuid, saksnummer);
                if (hendelseType == Hendelse.AVSLUTTET) {
                    // Henter inntektsmeldinger her pga sære caser der IM ikke behandles i fpsak rett etter journalføring.
                    // Feks ved IM på henlagte saker der det opprettes vurder dokument oppgave
                    lagreHentInntektsmeldingerSak(hendelseUuid, saksnummer);
                    //Henter vedlegg for å fjerne manglende vedlegg etter vedtak
                    lagreHentManglendeVedlegg(hendelseUuid, saksnummer);
                }
                if (hendelse.getKildesystem() == Kildesystem.FPTILBAKE) {
                    lagreHentTilbakekrevingTask(hendelseUuid, saksnummer, hendelseType);
                }
            }
        } catch (Exception e) {
            LOG.warn("Feilet ved håndtering av hendelse. Ignorerer {}", key, e);
        }
    }

    private void lagreHentInntektsmeldingerSak(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = opprettHentInntektsmeldingerTask(hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
    }

    private static ProsessTaskData opprettHentInntektsmeldingerTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = ProsessTaskData.forProsessTask(HentInntektsmeldingerTask.class);
        task.setCallId(hendelseUuid.toString());
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer + "-I");
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

    private void lagreHentSakTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = opprettHentSakTask(hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
    }

    public static ProsessTaskData opprettHentSakTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = ProsessTaskData.forProsessTask(HentSakTask.class);
        task.setCallId(hendelseUuid.toString());
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer.value());
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

    private void lagreHentTilbakekrevingTask(UUID hendelseUuid, Saksnummer saksnummer, Hendelse hendelse) {
        var task = opprettHentTilbakekrevingTask(hendelseUuid, saksnummer, hendelse);
        taskTjeneste.lagre(task);
    }

    private void lagreHentManglendeVedlegg(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = opprettHentManglendeVedleggTask(hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
    }

    public static ProsessTaskData opprettHentTilbakekrevingTask(UUID hendelseUuid, Saksnummer saksnummer, Hendelse hendelse) {
        var task = ProsessTaskData.forProsessTask(HentTilbakekrevingTask.class);
        task.setCallId(hendelseUuid.toString());
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer + "-T");
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        if (hendelse == Hendelse.VENTETILSTAND) { //Venter her for at fptilbake skal rekke å sende ut varslingsbrev
            task.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(60));
        }
        return task;
    }

    public static ProsessTaskData opprettHentManglendeVedleggTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = ProsessTaskData.forProsessTask(HentManglendeVedleggTask.class);
        task.setCallId(hendelseUuid.toString());
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer.value() + "-V");
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

    private static BehandlingHendelseV1 map(String payload) {
        return DefaultJsonMapper.fromJson(payload, BehandlingHendelseV1.class);
    }


}
