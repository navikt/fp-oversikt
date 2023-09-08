package no.nav.foreldrepenger.oversikt.saker;


import static no.nav.foreldrepenger.oversikt.saker.TilgangsstyringBorger.sjekkAtKallErFraBorger;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.AnnenPartVedtak;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;

@Path("/annenPart")
@ApplicationScoped
@Transactional
public class AnnenPartRest {

    private static final Logger LOG = LoggerFactory.getLogger(AnnenPartRest.class);

    private AnnenPartVedtakTjeneste annenPartVedtakTjeneste;
    private InnloggetBruker innloggetBruker;
    private AktørIdOppslag aktørIdOppslag;
    private AdresseBeskyttelseOppslag adresseBeskyttelseOppslag;

    @Inject
    public AnnenPartRest(AnnenPartVedtakTjeneste annenPartVedtakTjeneste, InnloggetBruker innloggetBruker, AktørIdOppslag aktørIdOppslag, AdresseBeskyttelseOppslag adresseBeskyttelseOppslag) {
        this.annenPartVedtakTjeneste = annenPartVedtakTjeneste;
        this.innloggetBruker = innloggetBruker;
        this.aktørIdOppslag = aktørIdOppslag;
        this.adresseBeskyttelseOppslag = adresseBeskyttelseOppslag;
    }

    AnnenPartRest() {
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public AnnenPartVedtak hent(@Valid @NotNull AnnenPartVedtakRequest request) {
        sjekkAtKallErFraBorger();
        if (harAnnenpartBeskyttetAdresseEllerFinnesIkkeIPDL(request.annenPartFødselsnummer())) {
            return null;
        }

        LOG.info("Kall mot annenPart endepunkt");
        var søkerAktørId = innloggetBruker.aktørId();
        var annenPartAktørId = aktørIdOppslag.forFnr(request.annenPartFødselsnummer());
        var barnAktørId = request.barnFødselsnummer() == null ? null : aktørIdOppslag.forFnr(request.barnFødselsnummer());
        var familieHendelse = request.familiehendelse();
        var vedtak = annenPartVedtakTjeneste.hentFor(søkerAktørId, annenPartAktørId, barnAktørId, familieHendelse);
        if (vedtak.isPresent()) {
            LOG.info("Returnerer annen parts vedtak. Antall perioder {} dekningsgrad {}", vedtak.get().perioder().size(), vedtak.get().dekningsgrad());
        }
        return vedtak.orElse(null);
    }

    private boolean harAnnenpartBeskyttetAdresseEllerFinnesIkkeIPDL(Fødselsnummer fnr) {
        AdresseBeskyttelse adresseBeskyttelse;
        try {
            adresseBeskyttelse = adresseBeskyttelseOppslag.adresseBeskyttelse(fnr);
        } catch (BrukerIkkeFunnetIPdlException e) {
            return true;
        }

        return adresseBeskyttelse.harBeskyttetAdresse();
    }

    record AnnenPartVedtakRequest(@Valid @NotNull Fødselsnummer annenPartFødselsnummer, @Valid Fødselsnummer barnFødselsnummer, LocalDate familiehendelse) {
    }
}
