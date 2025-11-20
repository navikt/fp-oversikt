package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentBeregningerTask.TASK_TYPE;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.oversikt.domene.beregning.Beregning;
import no.nav.foreldrepenger.oversikt.domene.beregning.BeregningRepository;
import no.nav.foreldrepenger.oversikt.domene.beregning.BeregningV1;
import no.nav.foreldrepenger.oversikt.innhenting.beregning.FpSakBeregningDto;

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
@ProsessTask(value = TASK_TYPE, maxFailedRuns = HentBeregningerTask.MAX_FAILED_RUNS, thenDelay = 60 * 15)
public class HentBeregningerTask implements ProsessTaskHandler {


    public static final String TASK_TYPE = "hent.beregninger";
    public static final Duration TASK_DELAY = Duration.ofSeconds(30);
    static final int MAX_FAILED_RUNS = 10;
    private static final Logger LOG = LoggerFactory.getLogger(HentBeregningerTask.class);

    private final FpsakTjeneste fpsakTjeneste;
    private final BeregningRepository beregningRepository;

    @Inject
    public HentBeregningerTask(FpsakTjeneste fpsakTjeneste, BeregningRepository beregningRepository) {
        this.fpsakTjeneste = fpsakTjeneste;
        this.beregningRepository = beregningRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        hentOgLagre(fpsakTjeneste, beregningRepository, saksnummer);
    }

    private static void hentOgLagre(FpsakTjeneste fpsakTjeneste,
                                    BeregningRepository repository,
                                    Saksnummer saksnummer) {
        var beregninger = fpsakTjeneste.hentBeregninger(saksnummer);
        LOG.info("Hentet beregninger for sak {} {}", saksnummer.value(), beregninger.size());

        Set<Beregning> mapped = beregninger.stream().map(HentBeregningerTask::map).collect(Collectors.toSet());

        if (beregninger.isEmpty()) {
            repository.slett(saksnummer);
        } else {
            repository.lagre(saksnummer, mapped);
        }
    }

    public static BeregningV1 map(FpSakBeregningDto fpSakBeregningDto) {
        return new BeregningV1(fpSakBeregningDto.dagsats());
    }

}
