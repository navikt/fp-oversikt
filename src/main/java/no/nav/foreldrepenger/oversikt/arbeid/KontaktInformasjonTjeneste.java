package no.nav.foreldrepenger.oversikt.arbeid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.digdir.KrrSpråkKlientSystem;

@ApplicationScoped
public class KontaktInformasjonTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(KontaktInformasjonTjeneste.class);

    private KrrSpråkKlientSystem krrSpråkKlientSystem;

    public KontaktInformasjonTjeneste() {
        // CDI
    }

    @Inject
    public KontaktInformasjonTjeneste(KrrSpråkKlientSystem krrSpråkKlientSystem) {
        this.krrSpråkKlientSystem = krrSpråkKlientSystem;
    }

    public boolean harReservertSegEllerKanIkkeVarsles(Fødselsnummer fnr) {
        try {
            var kontaktinformasjonOpt = krrSpråkKlientSystem.hentKontaktinformasjon(fnr.value());
            if (kontaktinformasjonOpt.isEmpty()) {
                return true;
            }
            var kontaktinformasjon = kontaktinformasjonOpt.get();
            if (!kontaktinformasjon.aktiv()) {
                return true;
            }
            return kontaktinformasjon.reservert() || !kontaktinformasjon.kanVarsles();
        } catch (Exception e) {
            LOG.warn("KrrSpråkKlient: kall til digdir krr feilet. Defaulter til at mor må dokumentere arbeid!", e);
            return true;
        }
    }
}
