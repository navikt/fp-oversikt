package no.nav.foreldrepenger.oversikt.arbeid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.digdir.KrrKlient;

@ApplicationScoped
public class KontaktInformasjonTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(KontaktInformasjonTjeneste.class);

    private KrrKlient krrKlient;

    public KontaktInformasjonTjeneste() {
        // CDI
    }

    @Inject
    public KontaktInformasjonTjeneste(KrrKlient krrKlient) {
        this.krrKlient = krrKlient;
    }

    public boolean harReservertSegEllerKanIkkeVarsles(Fødselsnummer fnr) {
        try {
            var kontaktinformasjonOpt = krrKlient.hentKontaktinformasjon(fnr.value());
            if (kontaktinformasjonOpt.isEmpty()) {
                LOG.info("KrrKlient: ingen kontaktinformasjon funnet på bruker");
                return true;
            }
            var kontaktinformasjon = kontaktinformasjonOpt.get();
            if (!kontaktinformasjon.aktiv()) {
                LOG.info("KrrKlient: kontaktinformasjon er inaktiv");
                return true;
            }
            return kontaktinformasjon.reservert() || !kontaktinformasjon.kanVarsles();
        } catch (Exception e) {
            LOG.warn("KrrKlient: kall til digdir krr feilet. Defaulter til at mor må dokumentere arbeid!", e);
            return true;
        }
    }
}
