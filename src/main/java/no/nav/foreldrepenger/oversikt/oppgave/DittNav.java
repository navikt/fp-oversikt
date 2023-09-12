package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
class DittNav {

    private static final Logger LOG = LoggerFactory.getLogger(DittNav.class);

    void avslutt(Fødselsnummer fødselsnummer, Saksnummer saksnummer, Set<Oppgave> oppgaver) {
        LOG.info("Avslutter ditt nav oppgaver {} {}", saksnummer, oppgaver);

    }

    public void opprett(Fødselsnummer fødselsnummer, Saksnummer saksnummer, Set<Oppgave> oppgaver) {
        LOG.info("Oppretter ditt nav oppgaver {} {}", saksnummer, oppgaver);
    }
}
