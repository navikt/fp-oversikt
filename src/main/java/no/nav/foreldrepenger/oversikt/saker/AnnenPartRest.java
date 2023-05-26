package no.nav.foreldrepenger.oversikt.saker;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.AnnenPartVedtak;
import no.nav.foreldrepenger.konfig.Environment;

@Path("/annenPart")
@ApplicationScoped
@Transactional
public class AnnenPartRest {

    private static final Environment ENV = Environment.current();
    private static final Logger LOG = LoggerFactory.getLogger(AnnenPartRest.class);

    private AnnenPartVedtakTjeneste annenPartVedtakTjeneste;
    private InnloggetBruker innloggetBruker;
    private AktørIdOppslag aktørIdOppslag;

    @Inject
    public AnnenPartRest(AnnenPartVedtakTjeneste annenPartVedtakTjeneste, InnloggetBruker innloggetBruker, AktørIdOppslag aktørIdOppslag) {
        this.annenPartVedtakTjeneste = annenPartVedtakTjeneste;
        this.innloggetBruker = innloggetBruker;
        this.aktørIdOppslag = aktørIdOppslag;
    }

    AnnenPartRest() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AnnenPartVedtak hent(@Valid @NotNull AnnenPartVedtakRequest request) {
        if (ENV.isProd()) {
            //Kan ikke svare i prod før autorisering er på plass
            return null;
        }
        LOG.info("Kall mot annenPart endepunkt");
        var søkerAktørId = innloggetBruker.aktørId();
        var annenPartAktørId = aktørIdOppslag.forFnr(request.annenPartFnr());
        var barnAktørId = aktørIdOppslag.forFnr(request.barnFnr());
        var familieHendelse = request.familieHendelse();
        var vedtak = annenPartVedtakTjeneste.hentFor(søkerAktørId, annenPartAktørId, barnAktørId, familieHendelse);
        if (vedtak.isPresent()) {
            LOG.info("Returnerer annen parts vedtak. Antall perioder {} dekningsgrad {}", vedtak.get().perioder().size(), vedtak.get().dekningsgrad());
        }
        return vedtak.orElse(null);
    }

    record AnnenPartVedtakRequest(@Valid @NotNull Fødselsnummer annenPartFnr, @Valid Fødselsnummer barnFnr, LocalDate familieHendelse) {
    }
}