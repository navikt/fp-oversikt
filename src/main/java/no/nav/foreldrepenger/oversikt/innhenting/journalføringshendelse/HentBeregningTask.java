package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentBeregningTask.TASK_TYPE;

import java.time.Duration;

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
@ProsessTask(value = TASK_TYPE, maxFailedRuns = HentBeregningTask.MAX_FAILED_RUNS, thenDelay = 60 * 15)
public class HentBeregningTask implements ProsessTaskHandler {


    public static final String TASK_TYPE = "hent.beregning";
    public static final Duration TASK_DELAY = Duration.ofSeconds(30);
    static final int MAX_FAILED_RUNS = 10;
    private static final Logger LOG = LoggerFactory.getLogger(HentBeregningTask.class);

    private final FpsakTjeneste fpsakTjeneste;
    private final BeregningRepository beregningRepository;

    @Inject
    public HentBeregningTask(FpsakTjeneste fpsakTjeneste, BeregningRepository beregningRepository) {
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
        var beregning = fpsakTjeneste.hentBeregning(saksnummer);
        LOG.info("Hentet beregning for sak {}", saksnummer.value());

        if (beregning.isEmpty()) {
            repository.slett(saksnummer);
        } else {
            repository.lagre(saksnummer, map(beregning.get()));
        }
    }

    // TODO -- AI generert. verifiser
    public static BeregningV1 map(FpSakBeregningDto fpSakBeregningDto) {
        var beregningsAndeler = fpSakBeregningDto.beregningsAndeler().stream()
            .map(andel -> new BeregningV1.BeregningsAndel(
                andel.aktivitetStatus(),
                andel.fastsattPrMnd(),
                BeregningV1.InntektsKilde.valueOf(andel.inntektsKilde().name()),
                andel.arbeidsforhold() != null
                    ? new BeregningV1.Arbeidsforhold(andel.arbeidsforhold().arbeidsgiverIdent(), andel.arbeidsforhold().refusjonPrMnd())
                    : null,
                andel.dagsats()
            ))
            .toList();

        var beregningAktivitetStatuser = fpSakBeregningDto.beregningAktivitetStatuser().stream()
            .map(status -> new BeregningV1.BeregningAktivitetStatus(status.aktivitetStatus(), status.hjemmel()))
            .toList();

        return new BeregningV1(fpSakBeregningDto.skjæringsTidspunkt(), beregningsAndeler, beregningAktivitetStatuser);
    }

}
