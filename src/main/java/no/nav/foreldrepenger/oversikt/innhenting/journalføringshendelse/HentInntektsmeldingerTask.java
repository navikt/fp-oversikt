package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingV1;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;
import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.Inntektsmelding;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "hent.inntektsmeldinger", maxFailedRuns = 10)
public class HentInntektsmeldingerTask implements ProsessTaskHandler {

    public static final Duration TASK_DELAY = Duration.ofSeconds(30);
    public static final String JOURNALPOST_ID = HentDataFraJoarkForHåndteringTask.JOURNALPOST_ID;
    private static final Logger LOG = LoggerFactory.getLogger(HentInntektsmeldingerTask.class);

    private final FpsakTjeneste fpsakTjeneste;
    private final InntektsmeldingerRepository inntektsmeldingerRepository;

    @Inject
    public HentInntektsmeldingerTask(FpsakTjeneste fpsakTjeneste, InntektsmeldingerRepository inntektsmeldingerRepository) {
        this.fpsakTjeneste = fpsakTjeneste;
        this.inntektsmeldingerRepository = inntektsmeldingerRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        var inntektsmeldinger = fpsakTjeneste.hentInntektsmeldinger(saksnummer);
        LOG.info("Hentet inntektsmeldinger for sak {} {}", saksnummer, inntektsmeldinger);
        var journalpostId = prosessTaskData.getPropertyValue(JOURNALPOST_ID);
        if (journalpostId != null && !imKnyttetTilJournalpost(inntektsmeldinger, journalpostId)) {
            throw new IntegrasjonException("FPOVERSIKT-IM",
                    "Finner ikke inntektsmelding med journalpostId " + journalpostId + " på sak " + saksnummer);
        }
        Set<no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.Inntektsmelding> mapped =
            inntektsmeldinger.stream().map(HentInntektsmeldingerTask::map).collect(Collectors.toSet());

        if (inntektsmeldinger.isEmpty()) {
            inntektsmeldingerRepository.slett(saksnummer);
        } else {
            inntektsmeldingerRepository.lagre(saksnummer, mapped);
        }
    }

    static InntektsmeldingV1 map(Inntektsmelding inntektsmelding) {
        return new InntektsmeldingV1(inntektsmelding.journalpostId(), inntektsmelding.arbeidsgiver(), inntektsmelding.innsendingstidspunkt(),
            inntektsmelding.inntekt());
    }

    private static boolean imKnyttetTilJournalpost(List<Inntektsmelding> inntektsmeldinger, String journalpostId) {
        return inntektsmeldinger.stream().anyMatch(i -> Objects.equals(i.journalpostId(), journalpostId));
    }
}
