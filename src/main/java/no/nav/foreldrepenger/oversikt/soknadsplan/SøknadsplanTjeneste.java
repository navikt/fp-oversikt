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
            LOG.info("Ignorerer søknad for sak {} som allerede er lagret som aktiv søknad", saksnummer.value());
            return;
        }
        if (aktivMottatt.filter(søknadsplan.mottattTidspunkt()::isBefore).isPresent()) {
            // Mottatt tidspunkt er lokal tid. Når klokka stilles tilbake til vintertid, kan en ny søknad derfor se eldre ut
            // enn en søknad sendt i timen før. Det er akseptert. Neste søknad retter planen.
            LOG.warn("Ignorerer søknad for sak {} som er eldre enn aktiv søknad. Kan skyldes overgang til vintertid", saksnummer.value());
            return;
        }
        søknadsplanRepository.lagre(saksnummer, søknadsplan);
    }
}
