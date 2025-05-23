package no.nav.foreldrepenger.oversikt.oppgave;

import static no.nav.foreldrepenger.oversikt.oppgave.MinSideBeskjedVedMottattSøknadTask.TASK_TYPE;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = TASK_TYPE, prioritet = 2)
public class MinSideBeskjedVedMottattSøknadTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MinSideBeskjedVedMottattSøknadTask.class);

    public static final String TASK_TYPE = "dittnav.beskjed";
    public static final String YTELSE_TYPE = "gjelderYtelsetype";
    public static final String ER_ENDRINGSSØKNAD = "erEndringssoknad";
    public static final String AKTØRID = "aktorid";
    public static final String EVENT_ID = "eventId";

    private MinSideTjeneste minSideTjeneste;

    @Inject
    MinSideBeskjedVedMottattSøknadTask(MinSideTjeneste minSideTjeneste) {
        this.minSideTjeneste = minSideTjeneste;
    }

    MinSideBeskjedVedMottattSøknadTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData data) {
        var aktørId = new AktørId(data.getPropertyValue(AKTØRID));
        var saksnummer = new Saksnummer(data.getSaksnummer());
        var erEndringssøknad = Boolean.parseBoolean(data.getPropertyValue(ER_ENDRINGSSØKNAD));
        var ytelseType = YtelseType.valueOf(data.getPropertyValue(YTELSE_TYPE));
        // eventuell ny innsending med samme eventId (kanalreferanse fra journalpost) regnes som duplikat og ignoreres av MinSideVarsel
        var eventId = UUID.fromString(data.getPropertyValue(EVENT_ID));
        minSideTjeneste.sendBeskjedVedInnkommetSøknad(aktørId, ytelseType, erEndringssøknad, eventId);
        LOG.info("Beskjed om mottatt søknad sendt for saksnummer {}", saksnummer.value());
    }

}
