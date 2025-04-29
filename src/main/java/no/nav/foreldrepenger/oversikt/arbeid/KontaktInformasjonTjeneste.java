package no.nav.foreldrepenger.oversikt.arbeid;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.digdir.KrrSpråkKlient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KontaktInformasjonTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(KontaktInformasjonTjeneste.class);
    private KrrSpråkKlient krrSpråkKlient;

    public KontaktInformasjonTjeneste() {
        // CDI
    }

    @Inject
    public KontaktInformasjonTjeneste(KrrSpråkKlient krrSpråkKlient) {
        this.krrSpråkKlient = krrSpråkKlient;
    }

    public boolean harReservertSegEllerKanIkkeVarsles(Fødselsnummer fnr) {
        try {
            var kontaktinformasjoner = krrSpråkKlient.hentKontaktinformasjon(fnr.value());
            if (kontaktinformasjoner == null || !kontaktinformasjoner.aktiv()) {
                return true;
            }
            return kontaktinformasjoner.reservert() || !kontaktinformasjoner.kanVarsles();
        } catch (Exception e) {
            LOG.warn("KrrSpråkKlient: kall til digdir krr feilet. Defaulter til at mor må dokumentere arbeid!", e);
            return true;
        }



    }
}
