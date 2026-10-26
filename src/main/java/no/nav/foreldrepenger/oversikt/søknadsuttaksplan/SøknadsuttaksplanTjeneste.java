package no.nav.foreldrepenger.oversikt.søknadsuttaksplan;

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
public class SøknadsuttaksplanTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadsuttaksplanTjeneste.class);

    private SøknadsuttaksplanRepository søknadsuttaksplanRepository;
    private SakRepository sakRepository;
    private UttaksplanTjeneste uttaksplanTjeneste;

    @Inject
    public SøknadsuttaksplanTjeneste(SøknadsuttaksplanRepository søknadsuttaksplanRepository, SakRepository sakRepository, UttaksplanTjeneste uttaksplanTjeneste) {
        this.søknadsuttaksplanRepository = søknadsuttaksplanRepository;
        this.sakRepository = sakRepository;
        this.uttaksplanTjeneste = uttaksplanTjeneste;
    }

    SøknadsuttaksplanTjeneste() {
        // CDI
    }

    /**
     * Førstegangssøknad erstatter alle lagrede perioder. Endringssøknad inneholder perioder fra første endrede dag
     * og slås sammen med felles uttaksplan. Avsender oppgir søknadstypen uavhengig av sakens vedtak.
     * Endringssøknader uten plangrunnlag ignoreres.
     */
    public void lagre(Saksnummer saksnummer, Søknadsuttaksplan søknadsuttaksplan, SøknadType søknadType) {
        Objects.requireNonNull(søknadType, "søknadType må være satt");
        var aktiv = søknadsuttaksplanRepository.hentAktivFor(saksnummer);
        if (aktiv.filter(a -> !a.mottattTidspunkt().isBefore(søknadsuttaksplan.mottattTidspunkt())).isPresent()) {
            LOG.info("Ignorerer søknad som ikke er nyere enn aktiv søknad på saken");
            return;
        }

        if (søknadType == SøknadType.FØRSTEGANGSSØKNAD) {
            søknadsuttaksplanRepository.lagre(saksnummer, søknadsuttaksplan);
            return;
        }

        var sak = sakRepository.finnFor(saksnummer).orElse(null);
        if (!(sak instanceof ForeldrepengerSak fpSak)) {
            LOG.info("Lagrer ikke endringssøknad fordi saken mangler plangrunnlag");
            return;
        }

        var grunnlag = uttaksplanTjeneste.hentFor(fpSak)
            .map(plan -> SøknadsuttaksplanMapper.tilPerioder(plan.perioder()))
            .filter(perioder -> !perioder.isEmpty());
        if (grunnlag.isEmpty()) {
            LOG.info("Lagrer ikke endringssøknad fordi saken mangler plangrunnlag");
            return;
        }

        var perioder = SøknadsuttaksplanPerioder.medEksisterendeFørEndringsdato(grunnlag.orElseThrow(), søknadsuttaksplan.perioder());
        søknadsuttaksplanRepository.lagre(saksnummer, new Søknadsuttaksplan(søknadsuttaksplan.mottattTidspunkt(), søknadsuttaksplan.rolle(), perioder));
    }
}
