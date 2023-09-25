package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentTilbakekrevingTask.MAX_FAILED_RUNS;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.TilbakekrevingV1;
import no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving.FptilbakeTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving.Tilbakekreving;
import no.nav.foreldrepenger.oversikt.oppgave.OppgaveTjeneste;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "hent.tilbakekreving", maxFailedRuns = MAX_FAILED_RUNS, firstDelay = 5)
public class HentTilbakekrevingTask implements ProsessTaskHandler {

    public static final String FORVENTER_BESVART_VARSEL = "forventerBesvartVarsel";

    private static final Logger LOG = LoggerFactory.getLogger(HentTilbakekrevingTask.class);

    static final int MAX_FAILED_RUNS = 4;

    private final FptilbakeTjeneste fptilbakeTjeneste;
    private final TilbakekrevingRepository tilbakekrevingRepository;
    private final OppgaveTjeneste oppgaveTjeneste;

    @Inject
    public HentTilbakekrevingTask(FptilbakeTjeneste fptilbakeTjeneste,
                                  TilbakekrevingRepository tilbakekrevingRepository,
                                  OppgaveTjeneste oppgaveTjeneste) {
        this.fptilbakeTjeneste = fptilbakeTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        var forventerBesvartVarsel = Boolean.parseBoolean(prosessTaskData.getPropertyValue(FORVENTER_BESVART_VARSEL));
        hentOgLagre(fptilbakeTjeneste, tilbakekrevingRepository, saksnummer, forventerBesvartVarsel, prosessTaskData.getAntallFeiledeForsøk());
        oppgaveTjeneste.opprettOppdaterOppgaveTask(saksnummer);
    }

    public static void hentOgLagre(FptilbakeTjeneste fptilbakeTjeneste,
                                   TilbakekrevingRepository repository,
                                   Saksnummer saksnummer,
                                   boolean forventerBesvartVarsel,
                                   int failedRuns) {
        fptilbakeTjeneste.hent(saksnummer).ifPresentOrElse(tilbakekreving -> {
            LOG.info("Hentet tilbakekreving for sak {}", saksnummer);
            if (forventerBesvartVarsel) {
                feilHvisVarselIkkeBesvart(saksnummer, failedRuns, tilbakekreving);
            }
            repository.lagre(map(tilbakekreving));
        }, () -> {
            LOG.info("Finner ingen tilbakekreving for sak {}", saksnummer);
            repository.slett(saksnummer);
        });
    }

    private static void feilHvisVarselIkkeBesvart(Saksnummer saksnummer, int failedRuns, Tilbakekreving tilbakekreving) {
        if (!harBesvartVarsel(tilbakekreving)) {
            if (failedRuns >= MAX_FAILED_RUNS - 1) {
                LOG.info("Feilet å hente tilbakekreving med besvart varsel etter {} forsøk på saksnummer {}", MAX_FAILED_RUNS, saksnummer);
            } else {
                throw new IntegrasjonException("FPOVERSIKT-VARSELRESPONS", "Finner ikke besvart varsel på sak " + saksnummer);
            }
        }
    }

    private static boolean harBesvartVarsel(Tilbakekreving tilbakekreving) {
        return tilbakekreving.varsel() != null && tilbakekreving.varsel().besvart();
    }

    static TilbakekrevingV1 map(Tilbakekreving tilbakekreving) {
        return new TilbakekrevingV1(new Saksnummer(tilbakekreving.saksnummer()), map(tilbakekreving.varsel()),
                tilbakekreving.harVerge(), LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }

    private static TilbakekrevingV1.Varsel map(Tilbakekreving.Varsel varsel) {
        if (varsel == null) {
            return null;
        }
        return new TilbakekrevingV1.Varsel(varsel.utsendtTidspunkt(), varsel.besvart());
    }
}
