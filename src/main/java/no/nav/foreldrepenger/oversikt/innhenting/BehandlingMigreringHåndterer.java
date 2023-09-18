package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende.ManglendeVedleggRepository;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentInntektsmeldingerTask;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentManglendeVedleggTask;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentTilbakekrevingTask;
import no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving.FptilbakeTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandlingMigreringHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingMigreringHåndterer.class);

    private ProsessTaskTjeneste taskTjeneste;
    private FpsakTjeneste fpSakKlient;
    private FptilbakeTjeneste fptilbakeTjeneste;
    private SakRepository sakRepository;
    private InntektsmeldingerRepository inntektsmeldingerRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private ManglendeVedleggRepository manglendeVedleggRepository;

    @Inject
    public BehandlingMigreringHåndterer(ProsessTaskTjeneste taskTjeneste,
                                        FpsakTjeneste fpSakKlient,
                                        FptilbakeTjeneste fptilbakeTjeneste,
                                        SakRepository sakRepository,
                                        InntektsmeldingerRepository inntektsmeldingerRepository,
                                        TilbakekrevingRepository tilbakekrevingRepository,
                                        ManglendeVedleggRepository manglendeVedleggRepository) {
        this.taskTjeneste = taskTjeneste;
        this.fpSakKlient = fpSakKlient;
        this.fptilbakeTjeneste = fptilbakeTjeneste;
        this.sakRepository = sakRepository;
        this.inntektsmeldingerRepository = inntektsmeldingerRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.manglendeVedleggRepository = manglendeVedleggRepository;
    }

    public BehandlingMigreringHåndterer() {

    }

    void handleMessage(String topic, String key, String payload) {
        LOG.info("Lest fra : topic={}", topic);
        try {
            var hendelse = map(payload);
//            hentSak(hendelse);
            hentInntektsmeldinger(hendelse);
            hentTilbakekreving(hendelse);
//            hentManglendeVedlegg(hendelse);
        } catch (Exception e) {
            LOG.warn("Feilet ved håndtering av hendelse. Ignorerer {}", key, e);
        }
    }

    private void hentInntektsmeldinger(BehandlingHendelseV1 hendelse) {
        var saksnummer = new Saksnummer(hendelse.getSaksnummer());
        try {
            HentInntektsmeldingerTask.hentOgLagre(fpSakKlient, inntektsmeldingerRepository, saksnummer);
        } catch (Exception e) {
            LOG.info("Direkte henting av inntektsmeldinger feilet {}", saksnummer.value(), e);
            lagreHentInntektsmeldingTask(hendelse.getHendelseUuid(), saksnummer);
        }
    }

    private void hentTilbakekreving(BehandlingHendelseV1 hendelse) {
        var saksnummer = new Saksnummer(hendelse.getSaksnummer());
        try {
            HentTilbakekrevingTask.hentOgLagre(fptilbakeTjeneste, tilbakekrevingRepository, saksnummer);
        } catch (Exception e) {
            LOG.info("Direkte henting av tilbakekreving feilet {}", saksnummer.value(), e);
            lagreHentTilbakekrevingTask(hendelse.getHendelseUuid(), saksnummer);
        }
    }

    private void hentManglendeVedlegg(BehandlingHendelseV1 hendelse) {
        var saksnummer = new Saksnummer(hendelse.getSaksnummer());
        try {
            HentManglendeVedleggTask.hentOgLagre(fpSakKlient, manglendeVedleggRepository, saksnummer);
        } catch (Exception e) {
            LOG.info("Direkte henting av manglende vedlegg feilet {}", saksnummer.value(), e);
            lagreHentManglendeVedleggTask(hendelse.getHendelseUuid(), saksnummer);
        }
    }

    private void hentSak(BehandlingHendelseV1 hendelse) {
        var saksnummer = new Saksnummer(hendelse.getSaksnummer());
        try {
            HentSakTask.hentOgLagreSak(fpSakKlient, sakRepository, saksnummer);
        } catch (Exception e) {
            LOG.info("Direkte henting av sak feilet {}", saksnummer.value(), e);
            lagreHentSakTask(hendelse.getHendelseUuid(), saksnummer);
        }
    }

    private ProsessTaskData opprettHentInntektsmeldingTask(UUID hendelseUuid, Saksnummer saksnummer) {
        return opprettTask(hendelseUuid, saksnummer, HentInntektsmeldingerTask.class);
    }

    private ProsessTaskData opprettHentTilbakekrevingTask(UUID hendelseUuid, Saksnummer saksnummer) {
        return opprettTask(hendelseUuid, saksnummer, HentTilbakekrevingTask.class);
    }

    private ProsessTaskData opprettHentManglendeVedleggTask(UUID hendelseUuid, Saksnummer saksnummer) {
        return opprettTask(hendelseUuid, saksnummer, HentManglendeVedleggTask.class);
    }

    private static ProsessTaskData opprettTask(UUID hendelseUuid, Saksnummer saksnummer, Class<? extends ProsessTaskHandler> clazz) {
        var task = ProsessTaskData.forProsessTask(clazz);
        task.setCallId(hendelseUuid.toString());
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer.value() + "-MIGRERING");
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

    private void lagreHentInntektsmeldingTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = opprettHentInntektsmeldingTask(hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
    }

    private void lagreHentTilbakekrevingTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = opprettHentTilbakekrevingTask(hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
    }

    private void lagreHentSakTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = opprettHentSakTask(hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
    }

    private void lagreHentManglendeVedleggTask(UUID hendelseUuid, Saksnummer saksnummer) {
        var task = opprettHentManglendeVedleggTask(hendelseUuid, saksnummer);
        taskTjeneste.lagre(task);
    }

    private ProsessTaskData opprettHentSakTask(UUID hendelseUuid, Saksnummer saksnummer) {
        return opprettTask(hendelseUuid, saksnummer, HentSakTask.class);
    }

    private static BehandlingHendelseV1 map(String payload) {
        return DefaultJsonMapper.fromJson(payload, BehandlingHendelseV1.class);
    }
}
