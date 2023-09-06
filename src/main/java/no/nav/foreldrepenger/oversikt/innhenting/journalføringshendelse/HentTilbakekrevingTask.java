package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.TilbakekrevingV1;
import no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving.FptilbakeTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving.Tilbakekreving;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
@ProsessTask("hent.tilbakekreving")
public class HentTilbakekrevingTask implements ProsessTaskHandler {

    public static final Duration TASK_DELAY = Duration.ofSeconds(10);

    private static final Logger LOG = LoggerFactory.getLogger(HentTilbakekrevingTask.class);

    private final FptilbakeTjeneste fptilbakeTjeneste;
    private final TilbakekrevingRepository tilbakekrevingRepository;

    @Inject
    public HentTilbakekrevingTask(FptilbakeTjeneste fptilbakeTjeneste, TilbakekrevingRepository tilbakekrevingRepository) {
        this.fptilbakeTjeneste = fptilbakeTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        fptilbakeTjeneste.hent(saksnummer).ifPresent(tilbakekreving -> {
            LOG.info("Hentet tilbakekreving {}", saksnummer);
            tilbakekrevingRepository.lagre(map(tilbakekreving));
        });
    }

    static TilbakekrevingV1 map(Tilbakekreving tilbakekreving) {
        return new TilbakekrevingV1(new Saksnummer(tilbakekreving.saksnummer()), map(tilbakekreving.varsel()),
                tilbakekreving.harVerge(), LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }

    private static TilbakekrevingV1.Varsel map(Tilbakekreving.Varsel varsel) {
        return new TilbakekrevingV1.Varsel(varsel.sendt(), varsel.besvart());
    }
}
