package no.nav.foreldrepenger.oversikt.oppgave;

import static no.nav.vedtak.felles.prosesstask.api.CommonTaskProperties.SAKSNUMMER;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.util.UUID;

@ApplicationScoped
@ProsessTask("dittnav.beskjed")
public class BrukernotifikasjonBeskjedVedMottattSøknadTask implements ProsessTaskHandler {

    public static final String YTELSE_TYPE = "gjelderYtelsetype";
    public static final String ER_ENDRINGSSØKNAD = "erEndringssoknad";
    public static final String FØDSELSNUMMER = "fodselsnummer";
    public static final String EVENT_ID = "eventId";
    private BrukernotifikasjonTjeneste brukernotifikasjonTjeneste;

    @Inject
    BrukernotifikasjonBeskjedVedMottattSøknadTask(BrukernotifikasjonTjeneste brukernotifikasjonTjeneste) {
        this.brukernotifikasjonTjeneste = brukernotifikasjonTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        var fnr = new Fødselsnummer(data.getPropertyValue(FØDSELSNUMMER));
        var saksnummer = new Saksnummer(data.getPropertyValue(SAKSNUMMER));
        var erEndringssøknad = Boolean.parseBoolean(data.getPropertyValue(ER_ENDRINGSSØKNAD));
        var ytelseType = YtelseType.valueOf(data.getPropertyValue(YTELSE_TYPE));
        // ved ny innsending med samme eventId (kanalreferanse fra journalpost) vil den regnes som duplikat av Brukernotifikasjon (ønsket resultat)
        // se https://tms-dokumentasjon.intern.nav.no/varsler/produsere#:~:text=NokkelInput
        var eventId = UUID.fromString(data.getPropertyValue(EVENT_ID));
        brukernotifikasjonTjeneste.sendBeskjedVedInnkommetSøknad(fnr, saksnummer, ytelseType, erEndringssøknad, eventId);
    }

}
