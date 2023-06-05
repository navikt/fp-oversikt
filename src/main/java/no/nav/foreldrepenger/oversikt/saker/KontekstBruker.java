package no.nav.foreldrepenger.oversikt.saker;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class KontekstBruker implements InnloggetBruker {

    private static final Logger LOG = LoggerFactory.getLogger(KontekstBruker.class);

    private PdlKlient pdlKlient;

    @Inject
    public KontekstBruker(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    KontekstBruker() {
        //CDI
    }

    @Override
    public AktørId aktørId() {
        var fnr = KontekstHolder.getKontekst().getUid();
        var aktørId = pdlKlient.hentAktørIdForPersonIdent(fnr).orElseThrow();
        LOG.info("Mapper fnr til aktørId");
        return new AktørId(aktørId);
    }

    @Override
    public boolean erMyndig() {
        var fnr = KontekstHolder.getKontekst().getUid();
        var fødselsdato = pdlKlient.fødselsdato(fnr);
        return fødselsdato.plusYears(18).isBefore(LocalDate.now());
    }
}
