package no.nav.foreldrepenger.oversikt.søknad;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTjeneste;

@ApplicationScoped
public class SøknadTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadTjeneste.class);

    private SøknadRepository søknadRepository;
    private SakRepository sakRepository;
    private UttaksplanTjeneste uttaksplanTjeneste;

    @Inject
    public SøknadTjeneste(SøknadRepository søknadRepository, SakRepository sakRepository, UttaksplanTjeneste uttaksplanTjeneste) {
        this.søknadRepository = søknadRepository;
        this.sakRepository = sakRepository;
        this.uttaksplanTjeneste = uttaksplanTjeneste;
    }

    SøknadTjeneste() {
        // CDI
    }

    /**
     * Førstegangssøknad erstatter alle lagrede perioder. Endringssøknad inneholder perioder fra første endrede dag
     * og slås sammen med felles uttaksplan. Avsender oppgir søknadstypen uavhengig av sakens vedtak.
     * Endringssøknader uten plangrunnlag ignoreres.
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

        var sak = sakRepository.finnFor(saksnummer).orElse(null);
        if (!(sak instanceof ForeldrepengerSak fpSak)) {
            LOG.info("Lagrer ikke endringssøknad fordi saken mangler plangrunnlag");
            return;
        }

        var grunnlag = uttaksplanTjeneste.hentFor(fpSak)
            .map(plan -> SøknadMapper.tilPerioder(plan.perioder()))
            .filter(perioder -> !perioder.isEmpty());
        if (grunnlag.isEmpty()) {
            LOG.info("Lagrer ikke endringssøknad fordi saken mangler plangrunnlag");
            return;
        }

        var perioder = SøknadPerioder.medEksisterendeFørEndringsdato(grunnlag.orElseThrow(), søknad.perioder());
        søknadRepository.lagre(saksnummer, new Søknad(søknad.mottattTidspunkt(), søknad.rolle(), perioder));
    }
}
