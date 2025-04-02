package no.nav.foreldrepenger.oversikt.oppgave;

import static java.time.ZoneOffset.UTC;
import static no.nav.tms.varsel.builder.OpprettVarselBuilder.eksternVarsling;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.tms.varsel.action.Sensitivitet;
import no.nav.tms.varsel.action.Varseltype;
import no.nav.tms.varsel.builder.InaktiverVarselBuilder;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;

@ApplicationScoped
class MinSideTjeneste {

    //https://navikt.github.io/tms-dokumentasjon/varsler/

    private static final Environment ENV = Environment.current();

    private static final Logger LOG = LoggerFactory.getLogger(MinSideTjeneste.class);
    private MinSideProducer producer;
    private URL innsynLenke;
    private URL morsArbeidLenke;
    private PersonOppslagSystem personOppslagSystem;
    private SakRepository sakRepository;

    @Inject
    public MinSideTjeneste(PersonOppslagSystem personOppslagSystem,
                           SakRepository sakRepository,
                           MinSideProducer producer,
                           @KonfigVerdi(value = "foreldrepenger.innsynlenke") String innsynLenke,
                           @KonfigVerdi(value = "foreldrepenger.morsarbeidlenke") String morsArbeidLenke
                           ) throws MalformedURLException {
        this.personOppslagSystem = personOppslagSystem;
        this.sakRepository = sakRepository;
        this.producer = producer;
        this.innsynLenke = URI.create(innsynLenke).toURL();
        this.morsArbeidLenke = URI.create(morsArbeidLenke).toURL();
    }

    MinSideTjeneste() {
        //CDI
    }

    void sendBeskjedVedInnkommetSøknad(AktørId aktørId,
                                              YtelseType ytelseType,
                                              boolean erEndringssøknad,
                                              UUID eventId) {
        String tekst;
        if (erEndringssøknad) {
            tekst = String.format("Vi mottok en søknad om endring av %s", ytelsetype(ytelseType));
        } else {
            tekst = String.format("Vi mottok en søknad om %s", ytelsetype(ytelseType));
        }
        var beskjedJson = beskjedJson(aktørId, eventId, tekst);
        producer.send(eventId, beskjedJson);
    }

    void opprett(Oppgave oppgave) {
        LOG.info("Oppretter brukernotifikasjonoppgave {}", oppgave);
        var sak = sakRepository.hentFor(oppgave.saksnummer());
        var oppgaveTekst = switch (oppgave.type()) {
            case LAST_OPP_MANGLENDE_VEDLEGG -> String.format("Det mangler vedlegg i søknaden din om %s", ytelsetype(sak.ytelse()));
        };
        var varselId = oppgave.id();
        var oppgaveJson = oppgaveJson(sak.aktørId(), varselId, oppgaveTekst);
        producer.send(varselId, oppgaveJson);
    }

    void avslutt(Oppgave oppgave) {
        LOG.info("Avslutter brukernotifikasjonoppgave {}", oppgave);
        var varselId = oppgave.id();
        var doneJson = doneJson(varselId);
        producer.send(varselId, doneJson);
    }

    private static String ytelsetype(YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> "foreldrepenger";
            case SVANGERSKAPSPENGER -> "svangerskapspenger";
            case ENGANGSSTØNAD -> "engangsstønad";
        };
    }

    private String beskjedJson(AktørId aktørId, UUID varselId, String beskjed) {
        var builder = builder(Varseltype.Beskjed, aktørId, varselId, innsynLenke.toString());
        builder.withTekst("nb", beskjed, true);
        builder.withAktivFremTil(LocalDateTime.now(UTC).plusDays(90).atZone(UTC));
        return builder.build();
    }

    private String oppgaveJson(AktørId aktørId, UUID varselId, String beskjed) {
        var builder = builder(Varseltype.Oppgave, aktørId, varselId, innsynLenke.toString());
        builder.withTekst("nb", beskjed, true);
        return builder.build();
    }

    private String doneJson(UUID varselId) {
        return InaktiverVarselBuilder.newInstance()
            .withVarselId(varselId.toString())
            .build();
    }

    private OpprettVarselBuilder builder(Varseltype type, AktørId aktørId, UUID varselId) {
        return builder(type, aktørId, varselId, null);
    }

    private OpprettVarselBuilder builder(Varseltype type, AktørId aktørId, UUID varselId, String lenke) {
        var fnr = personOppslagSystem.fødselsnummer(aktørId);
        var builder = OpprettVarselBuilder.newInstance();
        builder.withIdent(fnr.value());
        builder.withVarselId(varselId.toString());
        builder.withType(type);
        builder.withLink(lenke);
        builder.withSensitivitet(Sensitivitet.Substantial); // tilsvarer nivå 3
        return builder;
    }

    public void sendBeskjedMorsArbeid(AktørId morsAktørId, UUID eventId) {
        if (ENV.isProd()) {
            return; //TODO TFP-5383
        }

        var varselstekst = "Nav har brukt opplysninger om arbeidsforholdet ditt"; //TODO TFP-5383

        var builder = builder(Varseltype.Beskjed, morsAktørId, eventId)
            .withLink(morsArbeidLenke.toString())
            .withTekst("nb", varselstekst, true)
            .withEksternVarsling(eksternVarsling())
            .withAktivFremTil(LocalDateTime.now(UTC).plusDays(14).atZone(UTC));
        producer.send(eventId, builder.build());
    }
}
