package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hent.inntektsmeldinger")
public class HentInntektsmeldingerTask implements ProsessTaskHandler {

    public static final Duration TASK_DELAY = Duration.ofMinutes(1);
    private static final Logger LOG = LoggerFactory.getLogger(HentInntektsmeldingerTask.class);

    private final FpsakTjeneste fpsakTjeneste;

    @Inject
    public HentInntektsmeldingerTask(FpsakTjeneste fpsakTjeneste) {
        this.fpsakTjeneste = fpsakTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        var inntektsmeldinger = fpsakTjeneste.hentInntektsmeldinger(saksnummer);
        LOG.info("Hentet inntektsmeldinger for sak {} {}", saksnummer, inntektsmeldinger);
    }
}
