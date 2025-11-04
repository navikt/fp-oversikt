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
import no.nav.foreldrepenger.kontrakter.felles.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.kontrakter.fpoversikt.AnnenPartSak;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/annenPart")
@ApplicationScoped
@Transactional
public class AnnenPartRest {

    private static final Logger LOG = LoggerFactory.getLogger(AnnenPartRest.class);

    private AnnenPartSakTjeneste annenPartSakTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;
    private PersonOppslagSystem personOppslagSystem;

    @Inject
    public AnnenPartRest(AnnenPartSakTjeneste annenPartSakTjeneste, TilgangKontrollTjeneste tilgangkontroll, InnloggetBruker innloggetBruker,
                         PersonOppslagSystem personOppslagSystem) {
        this.annenPartSakTjeneste = annenPartSakTjeneste;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
        this.personOppslagSystem = personOppslagSystem;
    }

    AnnenPartRest() {
    }

    @Path("/aktorid")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public AktørId aktøridForPerson(@Valid @NotNull AnnenPartRequest annenPartRequest) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        return new AktørId(personOppslagSystem.aktørId(annenPartRequest.annenPartFødselsnummer()).value());
    }

    @Path("/v2")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public AnnenPartSak hent(@Valid @NotNull AnnenPartRest.AnnenPartRequest request) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        try {
            var adresseBeskyttelse = personOppslagSystem.adresseBeskyttelse(request.annenPartFødselsnummer());
            if (adresseBeskyttelse.harBeskyttetAdresse()) {
                return null;
            }
        } catch (BrukerIkkeFunnetIPdlException e) {
            LOG.info("Klarer ikke å finne adressebeskyttelse for annen part, person ikke funnet i pdl. Returnerer ingen sak for annen part", e);
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
        try {
            var adresseBeskyttelse = personOppslagSystem.adresseBeskyttelse(request.annenPartFødselsnummer());
            if (adresseBeskyttelse.harBeskyttetAdresse()) {
                return null;
            }
        } catch (BrukerIkkeFunnetIPdlException e) {
            LOG.info("Klarer ikke å finne adressebeskyttelse for annen part, person ikke funnet i pdl. Returnerer ingen vedtak for annen part", e);
            return null;
        }

        LOG.debug("Kall mot annenPart endepunkt");
        var søkerAktørId = innloggetBruker.aktørId();
        var annenPartAktørId = personOppslagSystem.aktørId(request.annenPartFødselsnummer());
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
