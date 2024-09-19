package no.nav.foreldrepenger.oversikt.saker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class OppslagInnloggetBruker implements InnloggetBruker {

    private static final Logger LOG = LoggerFactory.getLogger(OppslagInnloggetBruker.class);

    private PdlKlient pdlKlient;

    @Inject
    public OppslagInnloggetBruker(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    OppslagInnloggetBruker() {
        //CDI
    }

    @Override
    public AktørId aktørId() {
        var fnr = KontekstHolder.getKontekst().getUid();
        var aktørId = pdlKlient.hentAktørIdForPersonIdent(fnr).orElseThrow();
        LOG.debug("Mapper fnr til aktørId");
        return new AktørId(aktørId);
    }

    @Override
    public Fødselsnummer fødselsnummer() {
        return new Fødselsnummer(KontekstHolder.getKontekst().getUid());
    }
}
