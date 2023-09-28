package no.nav.foreldrepenger.oversikt.oppgave;

import static java.time.ZoneOffset.UTC;
import static no.nav.vedtak.log.mdc.MDCOperations.getCallId;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.saker.PdlKlientSystem;

@ApplicationScoped
public class BrukernotifikasjonTjeneste {

    //https://navikt.github.io/brukernotifikasjon-docs/

    private static final Logger LOG = LoggerFactory.getLogger(BrukernotifikasjonTjeneste.class);
    private static final Integer SIKKERHETSNIVÅ = 3;
    private static final String APPNAVN = "fpoversikt";
    private static final String NAMESPACE = "teamforeldrepenger";
    private URL innsynLenke;
    private BrukernotifikasjonProducer producer;
    private PdlKlientSystem pdlKlient;
    private SakRepository sakRepository;

    @Inject
    public BrukernotifikasjonTjeneste(PdlKlientSystem pdlKlient,
                                      SakRepository sakRepository,
                                      BrukernotifikasjonProducer brukernotifikasjonProducer,
                                      @KonfigVerdi(value = "dittnav.oppgave.lenke") String innsynLenke) throws MalformedURLException {
        this.pdlKlient = pdlKlient;
        this.sakRepository = sakRepository;
        this.producer = brukernotifikasjonProducer;
        this.innsynLenke = URI.create(innsynLenke).toURL();
    }

    BrukernotifikasjonTjeneste() {
        //CDI
    }

    public void sendBeskjedVedInnkommetSøknad(Fødselsnummer fnr, Saksnummer saksnummer, YtelseType ytelseType, boolean erEndringssøknad) {
        // ved ny innsending med samme eventId (callId/eksternReferanse fra journalpost) vil den regnes som duplikat av Brukernotifikasjon (ønsket resultat)
        var eventId = getCallId();
        var key = nøkkel(fnr, eventId, saksnummer);
        String tekst;
        if (erEndringssøknad) {
            tekst = String.format("Vi mottok en søknad om endring av %s", ytelsetype(ytelseType));
        } else {
            tekst = String.format("Vi mottok en søknad om %s", ytelsetype(ytelseType));
        }
        var beskjed = beskjed(tekst);
        producer.opprettBeskjed(beskjed, key);
    }

    public void opprett(Oppgave oppgave) {
        LOG.info("Oppretter brukernotifikasjonoppgave {}", oppgave);
        var sak = sakRepository.hentFor(oppgave.saksnummer());
        var brukernotifikasjonOppgave = switch (oppgave.type()) {
            case LAST_OPP_MANGLENDE_VEDLEGG -> oppgave(String.format("Det mangler vedlegg i søknaden din om %s", ytelsetype(sak.ytelse())));
            case SVAR_TILBAKEKREVING -> oppgave("Det mangler svar på varsel om tilbakebetaling");
        };

        var nøkkel = nøkkel(sak, oppgave);
        producer.opprettOppgave(brukernotifikasjonOppgave, nøkkel);
    }

    public void avslutt(Oppgave oppgave) {
        LOG.info("Avslutter brukernotifikasjonoppgave {}", oppgave);
        var sak = sakRepository.hentFor(oppgave.saksnummer());
        var nøkkel = nøkkel(sak, oppgave);
        producer.sendDone(done(), nøkkel);
    }

    private static String ytelsetype(YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> "foreldrepenger";
            case SVANGERSKAPSPENGER -> "svangerskapspenger";
            case ENGANGSSTØNAD -> "engangsstønad";
        };
    }

    private NokkelInput nøkkel(Sak sak, Oppgave oppgave) {
        var fnr = pdlKlient.hentFnrFor(sak.aktørId());
        var eventId = oppgave.id().toString();
        return nøkkel(fnr, eventId, sak.saksnummer());
    }

    private NokkelInput nøkkel(Fødselsnummer fnr, String eventId, Saksnummer saksnummer) {
        return new NokkelInputBuilder()
            .withFodselsnummer(fnr.value())
            .withEventId(eventId)
            .withAppnavn(APPNAVN)
            .withNamespace(NAMESPACE)
            .withGrupperingsId(saksnummer.value())
            .build();
    }

    private OppgaveInput oppgave(String tekst) {
        return new OppgaveInputBuilder()
            .withTidspunkt(LocalDateTime.now(UTC))
            .withLink(innsynLenke)
            .withSikkerhetsnivaa(SIKKERHETSNIVÅ)
            .withTekst(tekst)
            .build();
    }

    private BeskjedInput beskjed(String tekst) {
        return new BeskjedInputBuilder()
            .withTidspunkt(LocalDateTime.now(UTC))
            .withLink(innsynLenke)
            .withSikkerhetsnivaa(SIKKERHETSNIVÅ)
            .withSynligFremTil(LocalDateTime.now(UTC).plus(90, ChronoUnit.DAYS))
            .withTekst(tekst)
            .build();
    }

    private static DoneInput done() {
        return new DoneInputBuilder()
            .withTidspunkt(LocalDateTime.now(UTC))
            .build();
    }
}
