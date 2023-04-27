package no.nav.foreldrepenger.oversikt.saker;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.oversikt.VedtakRepository;

@ApplicationScoped
public class FpSaker {

    private static final Logger LOG = LoggerFactory.getLogger(FpSaker.class);

    private VedtakRepository vedtakRepository;

    @Inject
    public FpSaker(VedtakRepository vedtakRepository) {
        this.vedtakRepository = vedtakRepository;
    }

    FpSaker() {
        //CDI
    }

    public Object hent(String aktørId) {
        var vedtak = this.vedtakRepository.hentFor(aktørId);
        LOG.info("Hentet vedtak {}", vedtak);

        return null;
    }
}
