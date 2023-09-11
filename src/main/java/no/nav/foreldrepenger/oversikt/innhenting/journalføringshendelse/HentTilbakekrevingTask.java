package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import java.time.Duration;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

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

    public static String taskGruppeFor(String saksnummer) {
        return saksnummer + "-T";
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        fptilbakeTjeneste.hent(saksnummer).ifPresentOrElse(tilbakekreving -> {
            LOG.info("Hentet tilbakekreving for sak {}", saksnummer);
            tilbakekrevingRepository.lagre(map(tilbakekreving));
        }, () -> {
            LOG.info("Finner ingen tilbakekreving for sak {}", saksnummer);
            tilbakekrevingRepository.slett(saksnummer);
        });
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
