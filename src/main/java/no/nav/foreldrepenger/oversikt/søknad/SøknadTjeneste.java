package no.nav.foreldrepenger.oversikt.søknad;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@ApplicationScoped
public class SøknadTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadTjeneste.class);

    private SøknadRepository søknadRepository;

    @Inject
    public SøknadTjeneste(SøknadRepository søknadRepository) {
        this.søknadRepository = søknadRepository;
    }

    SøknadTjeneste() {
        // CDI
    }

    /**
     * Førstegangssøknad erstatter alle lagrede perioder. Endringssøknad inneholder perioder fra første endrede dag
     * og slås sammen med aktiv søknad. Avsender oppgir søknadstypen uavhengig av sakens vedtak.
     * Endringssøknader uten lagret søknad ignoreres.
     */
    public void lagre(Saksnummer saksnummer, Søknad søknad, SøknadType søknadType) {
        Objects.requireNonNull(søknadType, "søknadType må være satt");
        var aktiv = søknadRepository.hentAktivFor(saksnummer);
        if (aktiv.filter(a -> !a.mottattTidspunkt().isBefore(søknad.mottattTidspunkt())).isPresent()) {
            LOG.info("Ignorerer søknad som ikke er nyere enn aktiv søknad på saken");
            return;
        }

        if (søknadType == SøknadType.FØRSTEGANGSSØKNAD) {
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
}
