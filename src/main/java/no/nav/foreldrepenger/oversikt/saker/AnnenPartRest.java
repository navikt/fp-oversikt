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
import no.nav.foreldrepenger.common.innsyn.AnnenPartVedtak;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/annenPart")
@ApplicationScoped
@Transactional
public class AnnenPartRest {

    private static final Logger LOG = LoggerFactory.getLogger(AnnenPartRest.class);

    private AnnenPartVedtakTjeneste annenPartVedtakTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;
    private PersonOppslagSystem personOppslagSystem;

    @Inject
    public AnnenPartRest(AnnenPartVedtakTjeneste annenPartVedtakTjeneste, TilgangKontrollTjeneste tilgangkontroll, InnloggetBruker innloggetBruker,
                         PersonOppslagSystem personOppslagSystem) {
        this.annenPartVedtakTjeneste = annenPartVedtakTjeneste;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
        this.personOppslagSystem = personOppslagSystem;
    }

    AnnenPartRest() {
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public AnnenPartVedtak hent(@Valid @NotNull AnnenPartVedtakRequest request) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        if (personOppslagSystem.adresseBeskyttelse(request.annenPartFødselsnummer()).harBeskyttetAdresse()) {
            return null;
        }

        LOG.debug("Kall mot annenPart endepunkt");
        var søkerAktørId = innloggetBruker.aktørId();
        var annenPartAktørId = personOppslagSystem.aktørId(request.annenPartFødselsnummer());
        var barnAktørId = request.barnFødselsnummer() == null ? null : personOppslagSystem.aktørId(request.barnFødselsnummer());
        var familieHendelse = request.familiehendelse();
        var vedtak = annenPartVedtakTjeneste.hentFor(søkerAktørId, annenPartAktørId, barnAktørId, familieHendelse);
        if (vedtak.isPresent()) {
            LOG.info("Returnerer annen parts vedtak. Antall perioder {} dekningsgrad {}", vedtak.get().perioder().size(), vedtak.get().dekningsgrad());
        }
        return vedtak.orElse(null);
    }

    public record AnnenPartVedtakRequest(@Valid @NotNull Fødselsnummer annenPartFødselsnummer, @Valid Fødselsnummer barnFødselsnummer, LocalDate familiehendelse) {
    }
}
