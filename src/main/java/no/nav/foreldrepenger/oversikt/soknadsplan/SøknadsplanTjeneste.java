package no.nav.foreldrepenger.oversikt.soknadsplan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class SøknadsplanTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadsplanTjeneste.class);

    private SøknadsplanRepository søknadsplanRepository;

    @Inject
    public SøknadsplanTjeneste(SøknadsplanRepository søknadsplanRepository) {
        this.søknadsplanRepository = søknadsplanRepository;
    }

    SøknadsplanTjeneste() {
        // CDI
    }

    /**
     * Både førstegangssøknad og endringssøknad inneholder hele planen, og erstatter aktiv søknadsplan på saken.
     */
    public void lagre(Saksnummer saksnummer, Søknadsplan søknadsplan) {
        var aktivMottatt = søknadsplanRepository.hentAktivFor(saksnummer).map(Søknadsplan::mottattTidspunkt);
        if (aktivMottatt.filter(søknadsplan.mottattTidspunkt()::equals).isPresent()) {
            // Samme mottakstidspunkt betyr at samme søknad er sendt på nytt, f.eks. ved retry
            LOG.info("Ignorerer søknad som allerede er lagret som aktiv søknad på saken");
            return;
        }
        if (aktivMottatt.filter(søknadsplan.mottattTidspunkt()::isBefore).isPresent()) {
            LOG.warn("Ignorerer søknad som er eldre enn aktiv søknad på saken");
            return;
        }
        søknadsplanRepository.lagre(saksnummer, søknadsplan);
    }
}
