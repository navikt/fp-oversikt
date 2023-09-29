package no.nav.foreldrepenger.oversikt.oppgave;

import static no.nav.foreldrepenger.oversikt.oppgave.BrukernotifikasjonBeskjedVedMottattSøknadTask.TASK_TYPE;
import static no.nav.vedtak.felles.prosesstask.api.CommonTaskProperties.SAKSNUMMER;

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
@ProsessTask(TASK_TYPE)
public class BrukernotifikasjonBeskjedVedMottattSøknadTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BrukernotifikasjonBeskjedVedMottattSøknadTask.class);

    public static final String TASK_TYPE = "dittnav.beskjed";
    public static final String YTELSE_TYPE = "gjelderYtelsetype";
    public static final String ER_ENDRINGSSØKNAD = "erEndringssoknad";
    public static final String AKTØRID = "aktorid";
    public static final String EVENT_ID = "eventId";

    private BrukernotifikasjonTjeneste brukernotifikasjonTjeneste;

    @Inject
    BrukernotifikasjonBeskjedVedMottattSøknadTask(BrukernotifikasjonTjeneste brukernotifikasjonTjeneste) {
        this.brukernotifikasjonTjeneste = brukernotifikasjonTjeneste;
    }

    BrukernotifikasjonBeskjedVedMottattSøknadTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData data) {
        var aktørId = new AktørId(data.getPropertyValue(AKTØRID));
        var saksnummer = new Saksnummer(data.getPropertyValue(SAKSNUMMER));
        var erEndringssøknad = Boolean.parseBoolean(data.getPropertyValue(ER_ENDRINGSSØKNAD));
        var ytelseType = YtelseType.valueOf(data.getPropertyValue(YTELSE_TYPE));
        // ved ny innsending med samme eventId (kanalreferanse fra journalpost) vil den regnes som duplikat av Brukernotifikasjon (ønsket resultat)
        // se https://tms-dokumentasjon.intern.nav.no/varsler/produsere#:~:text=NokkelInput
        var eventId = UUID.fromString(data.getPropertyValue(EVENT_ID));
        brukernotifikasjonTjeneste.sendBeskjedVedInnkommetSøknad(aktørId, saksnummer, ytelseType, erEndringssøknad, eventId);
        LOG.info("Beskjed om mottatt søknad sendt for saksnummer {}", saksnummer.value());
    }

}
