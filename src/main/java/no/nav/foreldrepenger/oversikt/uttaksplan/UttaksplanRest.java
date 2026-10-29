package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.time.LocalDate;
import java.util.NoSuchElementException;

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
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

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
        var annenPart = verifisertAnnenPart(request.annenPartFødselsnummer());
        var barnAktørId = request.barnIdentifikator().fødselsnummer() == null ? null : personOppslagSystem.aktørId(request.barnIdentifikator()
                                                                                                                   .fødselsnummer());

        var uttaksplan = uttaksplanTjeneste.hentFor(søkerAktørId, annenPart, barnAktørId, request.barnIdentifikator().familiehendelse());
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
     * Annen part brukes bare når identen er kjent og adressen ikke er beskyttet. Ukjent ident, ukjent person
     * i PDL og beskyttet adresse gir samme resultat, slik at responsen ikke avslører adressebeskyttelse.
     * Tekniske feil mot PDL propagerer.
     */
    private VerifisertAnnenPart verifisertAnnenPart(Fødselsnummer annenPartFnr) {
        if (annenPartFnr == null) {
            return null;
        }
        AktørId aktørId;
        try {
            aktørId = personOppslagSystem.aktørId(annenPartFnr);
        } catch (NoSuchElementException e) {
            LOG.info("Finner ikke ident for annen part. Bygger uttaksplan uten annen part");
            return null;
        }
        if (aktørId == null || !harUbeskyttetAdresse(annenPartFnr)) {
            return null;
        }
        return new VerifisertAnnenPart(aktørId);
    }

    private boolean harUbeskyttetAdresse(Fødselsnummer fnr) {
        try {
            var beskyttelse = personOppslagSystem.adresseBeskyttelse(fnr);
            return beskyttelse != null && !beskyttelse.harBeskyttetAdresse();
        } catch (BrukerIkkeFunnetIPdlException e) {
            return false;
        }
    }

    public record FellesUttaksplanRequest(@NotNull @Valid BarnIdentifikator barnIdentifikator, @Valid Fødselsnummer annenPartFødselsnummer) {
    }

    public record BarnIdentifikator(@Valid Fødselsnummer fødselsnummer, LocalDate familiehendelse) { //Må ha minst en av feltene
    }
}
