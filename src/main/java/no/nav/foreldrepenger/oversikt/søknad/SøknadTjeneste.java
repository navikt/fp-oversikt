package no.nav.foreldrepenger.oversikt.søknad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;

@ApplicationScoped
public class SøknadTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadTjeneste.class);

    private SøknadRepository søknadRepository;
    private SakRepository sakRepository;

    @Inject
    public SøknadTjeneste(SøknadRepository søknadRepository, SakRepository sakRepository) {
        this.søknadRepository = søknadRepository;
        this.sakRepository = sakRepository;
    }

    SøknadTjeneste() {
        // CDI
    }

    /**
     * Førstegangssøknad erstatter alle lagrede perioder. Endringssøknad inneholder perioder fra første endrede dag
     * og slås sammen med aktiv søknad. Saken avgjør søknadstypen: kan bruker søke om endring, er det en endring.
     * Mangler saken, behandles søknaden som førstegangssøknad.
     */
    public void lagre(Saksnummer saksnummer, Søknad søknad) {
        var aktiv = søknadRepository.hentAktivFor(saksnummer);
        if (aktiv.filter(a -> !a.mottattTidspunkt().isBefore(søknad.mottattTidspunkt())).isPresent()) {
            LOG.info("Ignorerer søknad som ikke er nyere enn aktiv søknad på saken");
            return;
        }

        if (!erEndringssøknad(saksnummer)) {
            søknadRepository.lagre(saksnummer, søknad);
            return;
        }

        if (aktiv.isEmpty()) {
            // TODO: hent perioder fra gjeldende vedtak når endring kommer uten lagret søknad
            LOG.info("Lagrer ikke endringssøknad fordi saken mangler lagret søknad");
            return;
        }

        var perioder = SøknadPerioder.medEksisterendeFørEndringsdato(aktiv.get().perioder(), søknad.perioder());
        søknadRepository.lagre(saksnummer, new Søknad(søknad.mottattTidspunkt(), søknad.rolle(), perioder));
    }

    private boolean erEndringssøknad(Saksnummer saksnummer) {
        return sakRepository.finnFor(saksnummer)
            .filter(SakFP0.class::isInstance)
            .map(SakFP0.class::cast)
            .map(SakFP0::kanSøkeOmEndring)
            .orElse(false);
    }
}
