package no.nav.foreldrepenger.oversikt.oppgave;


import static no.nav.foreldrepenger.oversikt.oppgave.MinSideBeskjedMorsArbeidTask.TASK_TYPE;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = TASK_TYPE, prioritet = 3)
public class MinSideBeskjedMorsArbeidTask implements ProsessTaskHandler {

    public static final String TASK_TYPE = "dittnav.beskjed.morsarbeid";
    public static final Duration DELAY_MIN = Environment.current().isProd() ? Duration.ofMinutes(15) : Duration.ZERO;

    private static final Logger LOG = LoggerFactory.getLogger(MinSideBeskjedMorsArbeidTask.class);

    private MinSideTjeneste minSideTjeneste;
    private SakRepository sakRepository;

    @Inject
    MinSideBeskjedMorsArbeidTask(MinSideTjeneste minSideTjeneste, SakRepository sakRepository) {
        this.minSideTjeneste = minSideTjeneste;
        this.sakRepository = sakRepository;
    }

    MinSideBeskjedMorsArbeidTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData data) {
        var sak = sakRepository.hentFor(new Saksnummer(data.getSaksnummer()));
        var saksnummer = sak.saksnummer();
        if (sak.ytelse() != YtelseType.FORELDREPENGER) {
            LOG.warn("Sendte ikke beskjed for sak {}. Kan ikke sende beskjed om mors arbeid for sak med ytelse {}", saksnummer, sak.ytelse());
            return;
        }
        ForeldrepengerSak fpSak = (ForeldrepengerSak) sak;
        if (!fpSak.brukerRolle().erFarEllerMedmor()) {
            LOG.info("Sendte ikke beskjed for sak {}. Far/Medmor er ikke bruker", saksnummer);
            return;
        }
        var annenPartAktørId = fpSak.annenPartAktørId();
        if (annenPartAktørId == null) {
            LOG.info("Sendte ikke beskjed for sak {}. Annen part mangler", saksnummer);
            return;
        }
        var søknad = fpSak.sisteSøknad();
        if (søknad.isEmpty()) {
            throw new IllegalStateException("Søknad mangler for sak " + fpSak.saksnummer());
        }
        if (!søknad.get().morArbeidUtenDok()) {
            LOG.info("Sendte ikke beskjed for sak {}. Ingen søknad der mor skal varsles", saksnummer);
            return;
        }
        var nøkkel = saksnummer.value() + "beskjedMorsAktivitetArbeid"; //Legger på en streng her for å skille fra andre (framtidige) beskjeder på sak
        var eventId = UUID.nameUUIDFromBytes(nøkkel.getBytes(StandardCharsets.UTF_8)); //Samme saksummer gir samme uuid hver gang
        //Duplikat eventId ignoreres av MinSideVarsel. Hindrer flere utsendinger på samme sak
        minSideTjeneste.sendBeskjedMorsArbeid(annenPartAktørId, eventId);
        LOG.info("Beskjed om mors arbeid er sendt for saksnummer {}", saksnummer.value());
    }

}
