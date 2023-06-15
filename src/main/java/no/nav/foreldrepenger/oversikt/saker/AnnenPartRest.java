package no.nav.foreldrepenger.oversikt.saker;


import static no.nav.foreldrepenger.oversikt.saker.TilgangsstyringBorger.sjekkAtKallErFraBorger;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.AnnenPartVedtak;

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
        var annenpartsAdressebeskyttelse = adresseBeskyttelseOppslag.adresseBeskyttelse(request.annenPartFødselsnummer());
        if (annenpartsAdressebeskyttelse.isEmpty() || annenpartsAdressebeskyttelse.get().harBeskyttetAdresse()) {
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

    record AnnenPartVedtakRequest(@Valid Fødselsnummer annenPartFødselsnummer, @Valid Fødselsnummer barnFødselsnummer, LocalDate familiehendelse) {
    }
}
