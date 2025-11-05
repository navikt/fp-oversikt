package no.nav.foreldrepenger.oversikt.saker;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
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
        LOG.debug("Mapper fnr til aktørId");
        var fnr = KontekstHolder.getKontekst().getUid();
        return pdlKlient.aktørId(fnr);
    }

    @Override
    public Fødselsnummer fødselsnummer() {
        return new Fødselsnummer(KontekstHolder.getKontekst().getUid());
    }

    @Override
    public boolean erMyndig() {
        var fnr = KontekstHolder.getKontekst().getUid();
        var fødselsdato = pdlKlient.fødselsdato(fnr);
        return fødselsdato.plusYears(18).isBefore(LocalDate.now());
    }
}
