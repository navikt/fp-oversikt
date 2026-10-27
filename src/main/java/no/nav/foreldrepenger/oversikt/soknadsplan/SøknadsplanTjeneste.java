package no.nav.foreldrepenger.oversikt.soknadsplan;

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
public class SøknadsplanTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadsplanTjeneste.class);

    private SøknadsplanRepository søknadsplanRepository;
    private SakRepository sakRepository;
    private UttaksplanTjeneste uttaksplanTjeneste;

    @Inject
    public SøknadsplanTjeneste(SøknadsplanRepository søknadsplanRepository, SakRepository sakRepository, UttaksplanTjeneste uttaksplanTjeneste) {
        this.søknadsplanRepository = søknadsplanRepository;
        this.sakRepository = sakRepository;
        this.uttaksplanTjeneste = uttaksplanTjeneste;
    }

    SøknadsplanTjeneste() {
        // CDI
    }

    /**
     * Førstegangssøknad erstatter alle lagrede perioder. Endringssøknad inneholder perioder fra første endrede dag
     * og slås sammen med felles uttaksplan. Avsender oppgir søknadstypen uavhengig av sakens vedtak.
     * Endringssøknader uten plangrunnlag ignoreres.
     */
    public void lagre(Saksnummer saksnummer, Søknadsplan søknadsplan, SøknadType søknadType) {
        Objects.requireNonNull(søknadType, "søknadType må være satt");
        var aktiv = søknadsplanRepository.hentAktivFor(saksnummer);
        if (aktiv.filter(a -> !a.mottattTidspunkt().isBefore(søknadsplan.mottattTidspunkt())).isPresent()) {
            LOG.info("Ignorerer søknad som ikke er nyere enn aktiv søknad på saken");
            return;
        }

        if (søknadType == SøknadType.FØRSTEGANGSSØKNAD) {
            søknadsplanRepository.lagre(saksnummer, søknadsplan);
            return;
        }

        var sak = sakRepository.finnFor(saksnummer).orElse(null);
        if (!(sak instanceof ForeldrepengerSak fpSak)) {
            LOG.info("Lagrer ikke endringssøknad fordi saken mangler plangrunnlag");
            return;
        }

        var grunnlag = uttaksplanTjeneste.hentFor(fpSak)
            .map(plan -> SøknadsplanMapper.tilPerioder(plan.perioder()))
            .filter(perioder -> !perioder.isEmpty());
        if (grunnlag.isEmpty()) {
            LOG.info("Lagrer ikke endringssøknad fordi saken mangler plangrunnlag");
            return;
        }

        var perioder = SøknadsplanPerioder.medEksisterendeFørEndringsdato(grunnlag.orElseThrow(), søknadsplan.perioder());
        søknadsplanRepository.lagre(saksnummer, new Søknadsplan(søknadsplan.mottattTidspunkt(), søknadsplan.rolle(), perioder));
    }
}
