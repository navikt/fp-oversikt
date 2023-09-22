package no.nav.foreldrepenger.oversikt.oppgave;

import static java.time.ZoneOffset.UTC;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;

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
class DittNav {

    //https://navikt.github.io/brukernotifikasjon-docs/

    private static final Logger LOG = LoggerFactory.getLogger(DittNav.class);
    private static final Integer SIKKERHETSNIVÅ = 3;
    private static final String APPNAVN = "fpoversikt";
    private static final String NAMESPACE = "teamforeldrepenger";

    private URL oppgaveLenke;
    private DittNavProducer producer;
    private PdlKlientSystem pdlKlient;
    private SakRepository sakRepository;

    @Inject
    DittNav(PdlKlientSystem pdlKlient,
            SakRepository sakRepository,
            DittNavProducer dittNavProducer,
            @KonfigVerdi(value = "dittnav.oppgave.lenke") String oppgaveLenke) throws MalformedURLException {
        this.pdlKlient = pdlKlient;
        this.sakRepository = sakRepository;
        this.producer = dittNavProducer;
        this.oppgaveLenke = URI.create(oppgaveLenke).toURL();
    }

    DittNav() {
        //CDI
    }

    void opprett(Oppgave oppgave) {
        LOG.info("Oppretter ditt nav oppgave {}", oppgave);
        var sak = sakRepository.hentFor(oppgave.saksnummer());
        var dittNavOppgaveInput = switch (oppgave.type()) {
            case LAST_OPP_MANGLENDE_VEDLEGG -> opprettOppgaveInput(String.format("Det mangler vedlegg i søknaden din om %s", ytelsetype(sak)));
            case SVAR_TILBAKEKREVING -> opprettOppgaveInput("Det mangler svar på varsel om tilbakebetaling");
        };

        var nøkkel = nøkkel(sak, oppgave);
        producer.sendOpprettOppgave(dittNavOppgaveInput, nøkkel);
        LOG.info("Opprettet ditt nav oppgave {}", oppgave);
    }

    void avslutt(Oppgave oppgave) {
        LOG.info("Avslutter ditt nav oppgave {}", oppgave);
        var sak = sakRepository.hentFor(oppgave.saksnummer());
        var nøkkel = nøkkel(sak, oppgave);
        producer.sendAvsluttOppgave(avsluttOppgaveInput(), nøkkel);
        LOG.info("Avsluttet ditt nav oppgave {}", oppgave);
    }

    private NokkelInput nøkkel(Sak sak, Oppgave oppgave) {
        var fnr = pdlKlient.hentFnrFor(sak.aktørId());
        return new NokkelInputBuilder()
            .withFodselsnummer(fnr.value())
            .withEventId(oppgave.id().toString())
            .withAppnavn(APPNAVN)
            .withNamespace(NAMESPACE)
            .withGrupperingsId(sak.saksnummer().value())
            .build();
    }

    private static String ytelsetype(Sak sak) {
        return switch (sak.ytelse()) {
            case FORELDREPENGER -> "foreldrepenger";
            case SVANGERSKAPSPENGER -> "svangerskapspenger";
            case ENGANGSSTØNAD -> "engangsstønad";
        };
    }

    private OppgaveInput opprettOppgaveInput(String tekst) {
        return new OppgaveInputBuilder()
            .withTidspunkt(LocalDateTime.now(UTC))
            .withLink(oppgaveLenke)
            .withSikkerhetsnivaa(SIKKERHETSNIVÅ)
            .withTekst(tekst)
            .build();
    }

    private static DoneInput avsluttOppgaveInput() {
        return new DoneInputBuilder()
            .withTidspunkt(LocalDateTime.now(UTC))
            .build();
    }
}
