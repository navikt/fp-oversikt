package no.nav.foreldrepenger.oversikt.saker;


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
import no.nav.foreldrepenger.common.innsyn.AnnenPartSak;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontroll;

@Path("/annenPart")
@ApplicationScoped
@Transactional
public class AnnenPartRest {

    private static final Logger LOG = LoggerFactory.getLogger(AnnenPartRest.class);

    private AnnenPartSakTjeneste annenPartSakTjeneste;
    private TilgangKontroll tilgangkontroll;
    private InnloggetBruker innloggetBruker;
    private PersonOppslagSystem personOppslagSystem;

    @Inject
    public AnnenPartRest(AnnenPartSakTjeneste annenPartSakTjeneste, TilgangKontroll tilgangkontroll, InnloggetBruker innloggetBruker,
                         PersonOppslagSystem personOppslagSystem) {
        this.annenPartSakTjeneste = annenPartSakTjeneste;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
        this.personOppslagSystem = personOppslagSystem;
    }

    AnnenPartRest() {
    }

    @Path("/v2")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public AnnenPartSak hent(@Valid @NotNull AnnenPartRest.AnnenPartRequest request) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        var annenpartsFødselsnummer = request.annenPartFødselsnummer();
        if (tilgangkontroll.erUkjentPerson(annenpartsFødselsnummer.value()) || tilgangkontroll.harAdresseBeskyttelse(annenpartsFødselsnummer.value())) {
            return null;
        }

        var søkerAktørId = innloggetBruker.aktørId();
        var annenPartAktørId = personOppslagSystem.aktørId(request.annenPartFødselsnummer());
        var barnAktørId = request.barnFødselsnummer() == null ? null : personOppslagSystem.aktørId(request.barnFødselsnummer());
        var familieHendelse = request.familiehendelse();
        var annenPartSak = annenPartSakTjeneste.hentFor(søkerAktørId, annenPartAktørId, barnAktørId, familieHendelse);
        if (annenPartSak.isPresent()) {
            LOG.info("Returnerer annen parts sak. Antall perioder {} dekningsgrad {}", annenPartSak.get().perioder().size(), annenPartSak.get().dekningsgrad());
        }
        return annenPartSak.orElse(null);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public AnnenPartSak hentVedtak(@Valid @NotNull AnnenPartRest.AnnenPartRequest request) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        var annenpartsFødselsnummer = request.annenPartFødselsnummer();
        if (tilgangkontroll.erUkjentPerson(annenpartsFødselsnummer.value()) || tilgangkontroll.harAdresseBeskyttelse(annenpartsFødselsnummer.value())) {
            return null;
        }

        LOG.debug("Kall mot annenPart endepunkt");
        var søkerAktørId = innloggetBruker.aktørId();
        var annenPartAktørId = personOppslagSystem.aktørId(annenpartsFødselsnummer);
        var barnAktørId = request.barnFødselsnummer() == null ? null : personOppslagSystem.aktørId(request.barnFødselsnummer());
        var familieHendelse = request.familiehendelse();
        var vedtak = annenPartSakTjeneste.hentVedtak(søkerAktørId, annenPartAktørId, barnAktørId, familieHendelse);
        if (vedtak.isPresent()) {
            LOG.info("Returnerer annen parts vedtak. Antall perioder {} dekningsgrad {}", vedtak.get().perioder().size(), vedtak.get().dekningsgrad());
        }
        return vedtak.orElse(null);
    }

    public record AnnenPartRequest(@Valid @NotNull Fødselsnummer annenPartFødselsnummer, @Valid Fødselsnummer barnFødselsnummer, LocalDate familiehendelse) {
    }
}
