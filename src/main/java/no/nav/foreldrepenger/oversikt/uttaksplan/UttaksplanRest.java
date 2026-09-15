package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

/**
 * Felles uttaksplan for søker og annen part.
 */
@Path("/uttaksplan")
@ApplicationScoped
@Transactional
public class UttaksplanRest {

    private static final Logger LOG = LoggerFactory.getLogger(UttaksplanRest.class);

    private UttaksplanTjeneste uttaksplanTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;
    private PersonOppslagSystem personOppslagSystem;

    @Inject
    public UttaksplanRest(UttaksplanTjeneste uttaksplanTjeneste,
                          TilgangKontrollTjeneste tilgangkontroll,
                          InnloggetBruker innloggetBruker,
                          PersonOppslagSystem personOppslagSystem) {
        this.uttaksplanTjeneste = uttaksplanTjeneste;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
        this.personOppslagSystem = personOppslagSystem;
    }

    UttaksplanRest() {
        //CDI
    }

    /**
     * Manglende sak, skjermet adresse og manglende relasjon skal gi samme respons
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public FellesUttaksplanDto hent(@Valid @NotNull FellesUttaksplanRequest request) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        validerBarnIdentifikator(request.barnIdentifikator());

        var søkerAktørId = innloggetBruker.aktørId();
        var annenPartAktørId = annenPartAktørId(request);
        var barnAktørId = request.barnIdentifikator().fødselsnummer() == null ? null : personOppslagSystem.aktørId(request.barnIdentifikator()
                                                                                                                   .fødselsnummer());

        var uttaksplan = uttaksplanTjeneste.hentFor(søkerAktørId, annenPartAktørId, barnAktørId, request.barnIdentifikator().familiehendelse());
        LOG.info("Returnerer felles uttaksplan med {} perioder", uttaksplan.map(u -> u.perioder().size()).orElse(0));
        return uttaksplan.orElse(null);
    }

    /**
     * Uten fødselsnummer eller familiehendelse finnes det ingen sak å matche mot, og klienten
     * ville fått en tom plan uten å få vite hvorfor.
     */
    private static void validerBarnIdentifikator(BarnIdentifikator barnIdentifikator) {
        if (barnIdentifikator.fødselsnummer() == null && barnIdentifikator.familiehendelse() == null) {
            throw new BadRequestException("Barn må identifiseres med fødselsnummer eller familiehendelse");
        }
    }

    /**
     * Returnerer null når annen part er skjermet eller ukjent. Planen bygges da uten annen part.
     */
    private AktørId annenPartAktørId(FellesUttaksplanRequest request) {
        if (request.annenPartFødselsnummer() == null) {
            return null;
        }
        try {
            var adresseBeskyttelse = personOppslagSystem.adresseBeskyttelse(request.annenPartFødselsnummer());
            if (adresseBeskyttelse.harBeskyttetAdresse()) {
                return null;
            }
        } catch (BrukerIkkeFunnetIPdlException e) {
            LOG.info("Finner ikke adressebeskyttelse for annen part. Bygger uttaksplan uten annen part", e);
            return null;
        }
        return personOppslagSystem.aktørId(request.annenPartFødselsnummer());
    }

    public record FellesUttaksplanRequest(@NotNull @Valid BarnIdentifikator barnIdentifikator, @Valid Fødselsnummer annenPartFødselsnummer) {
    }

    public record BarnIdentifikator(@Valid Fødselsnummer fødselsnummer, LocalDate familiehendelse) { //Må ha minst en av feltene
    }
}
