package no.nav.foreldrepenger.oversikt.oppgave;

import static java.time.ZoneOffset.UTC;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.tms.varsel.action.Sensitivitet;
import no.nav.tms.varsel.action.Varseltype;
import no.nav.tms.varsel.builder.InaktiverVarselBuilder;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;

@ApplicationScoped
public class BrukernotifikasjonTjeneste {

    //https://navikt.github.io/brukernotifikasjon-docs/

    private static final Logger LOG = LoggerFactory.getLogger(BrukernotifikasjonTjeneste.class);
    private static final Integer SIKKERHETSNIVÅ = 3;
    private static final String APPNAVN = "fpoversikt";
    private static final String NAMESPACE = "teamforeldrepenger";
    private MinSideVarselProducer producer;
    private URL innsynLenke;
    private BrukernotifikasjonProducer legacyProducer;
    private PersonOppslagSystem personOppslagSystem;
    private SakRepository sakRepository;

    @Inject
    public BrukernotifikasjonTjeneste(PersonOppslagSystem personOppslagSystem,
                                      SakRepository sakRepository,
                                      BrukernotifikasjonProducer brukernotifikasjonProducer,
                                      MinSideVarselProducer producer,
                                      @KonfigVerdi(value = "foreldrepenger.innsynlenke") String innsynLenke) throws MalformedURLException {
        this.personOppslagSystem = personOppslagSystem;
        this.sakRepository = sakRepository;
        this.legacyProducer = brukernotifikasjonProducer;
        this.producer = producer;
        this.innsynLenke = URI.create(innsynLenke).toURL();
    }

    BrukernotifikasjonTjeneste() {
        //CDI
    }

    public void sendBeskjedVedInnkommetSøknad(AktørId aktørId,
                                              Saksnummer saksnummer,
                                              YtelseType ytelseType,
                                              boolean erEndringssøknad,
                                              UUID eventId) {
        var fnr = personOppslagSystem.fødselsnummer(aktørId);
        String tekst;
        if (erEndringssøknad) {
            tekst = String.format("Vi mottok en søknad om endring av %s", ytelsetype(ytelseType));
        } else {
            tekst = String.format("Vi mottok en søknad om %s", ytelsetype(ytelseType));
        }
        if (Environment.current().isProd()) {
            var beskjed = legacyBeskjed(tekst);
            var key = legacyNøkkel(fnr, eventId.toString(), saksnummer);
            legacyProducer.opprettBeskjed(beskjed, key);
        } else {
            var beskjedJson = beskjedJson(aktørId, eventId, tekst);
            producer.send(eventId, beskjedJson);
        }
    }

    public void opprett(Oppgave oppgave) {
        LOG.info("Oppretter brukernotifikasjonoppgave {}", oppgave);
        var sak = sakRepository.hentFor(oppgave.saksnummer());
        var oppgaveTekst = switch (oppgave.type()) {
            case LAST_OPP_MANGLENDE_VEDLEGG -> String.format("Det mangler vedlegg i søknaden din om %s", ytelsetype(sak.ytelse()));
        };

        if (Environment.current().isProd()) {
            var legacyNøkkel = legacyNøkkel(sak, oppgave, sak.aktørId());
            var legacyOppgaveInput = legacyOppgave(oppgaveTekst);
            legacyProducer.opprettOppgave(legacyOppgaveInput, legacyNøkkel);
        } else {
            var varselId = oppgave.id();
            var oppgaveJson = oppgaveJson(sak.aktørId(), varselId, oppgaveTekst);
            producer.send(varselId, oppgaveJson);
        }
    }

    public void avslutt(Oppgave oppgave) {
        LOG.info("Avslutter brukernotifikasjonoppgave {}", oppgave);
        var sak = sakRepository.hentFor(oppgave.saksnummer());
        if (Environment.current().isProd()) {
            var legacyNøkkel = legacyNøkkel(sak, oppgave, sak.aktørId());
            legacyProducer.sendDone(legacyDone(), legacyNøkkel);
        } else {
            var varselId = oppgave.id();
            var doneJson = doneJson(varselId);
            producer.send(varselId, doneJson);
        }
    }

    private static String ytelsetype(YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> "foreldrepenger";
            case SVANGERSKAPSPENGER -> "svangerskapspenger";
            case ENGANGSSTØNAD -> "engangsstønad";
        };
    }

    private String beskjedJson(AktørId aktørId, UUID varselId, String beskjed) {
        var builder = builder(Varseltype.Beskjed, aktørId, varselId);
        builder.withTekst("nb", beskjed, true);
        builder.withAktivFremTil(beskjedVarighet());
        // todo: verifiser at det ikke går eksternVarsling
        return builder.build();
    }

    private String oppgaveJson(AktørId aktørId, UUID varselId, String beskjed) {
        var builder = builder(Varseltype.Oppgave, aktørId, varselId);
        builder.withTekst("nb", beskjed, true);
        return builder.build();
    }

    private String doneJson(UUID varselId) {
        var builder = InaktiverVarselBuilder.newInstance();
        builder.withVarselId(varselId.toString());
        return builder.build();
    }

    private OpprettVarselBuilder builder(Varseltype type, AktørId aktørId, UUID varselId) {
        var fnr = personOppslagSystem.fødselsnummer(aktørId);
        var builder = OpprettVarselBuilder.newInstance();
        builder.withIdent(fnr.value());
        builder.withVarselId(varselId.toString());
        builder.withType(type);
        builder.withLink(innsynLenke.toString());
        builder.withSensitivitet(Sensitivitet.Substantial); // tilsvarer nivå 3
        return builder;
    }

    private NokkelInput legacyNøkkel(Sak sak, Oppgave oppgave, AktørId aktørId) {
        var fnr = personOppslagSystem.fødselsnummer(aktørId);
        var eventId = oppgave.id().toString();
        return legacyNøkkel(fnr, eventId, sak.saksnummer());
    }

    private NokkelInput legacyNøkkel(Fødselsnummer fnr, String eventId, Saksnummer saksnummer) {
        return new NokkelInputBuilder()
            .withFodselsnummer(fnr.value())
            .withEventId(eventId)
            .withAppnavn(APPNAVN)
            .withNamespace(NAMESPACE)
            .withGrupperingsId(saksnummer.value())
            .build();
    }

    private OppgaveInput legacyOppgave(String tekst) {
        return new OppgaveInputBuilder()
            .withTidspunkt(LocalDateTime.now(UTC))
            .withLink(innsynLenke)
            .withSikkerhetsnivaa(SIKKERHETSNIVÅ)
            .withTekst(tekst)
            .build();
    }

    private BeskjedInput legacyBeskjed(String tekst) {
        return new BeskjedInputBuilder()
            .withTidspunkt(LocalDateTime.now(UTC))
            .withLink(innsynLenke)
            .withSikkerhetsnivaa(SIKKERHETSNIVÅ)
            .withSynligFremTil(LocalDateTime.now(UTC).plusDays(90))
            .withTekst(tekst)
            .build();
    }

    private static DoneInput legacyDone() {
        return new DoneInputBuilder()
            .withTidspunkt(LocalDateTime.now(UTC))
            .build();
    }

    private static ZonedDateTime beskjedVarighet() {
        return LocalDateTime.now(UTC).plusDays(90).atZone(UTC);
    }
}
