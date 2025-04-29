package no.nav.foreldrepenger.oversikt.arbeid;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.oppslag.MineArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@Path("/arbeid")
@ApplicationScoped
@Transactional
public class ArbeidRest {

    private static final Logger LOG = LoggerFactory.getLogger(ArbeidRest.class);

    private AnnenPartSakTjeneste annenPartSakTjeneste;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;
    private PersonOppslagSystem personOppslagSystem;
    private MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste;
    private AktivitetskravArbeidDokumentasjonsKravTjeneste aktivitetskravArbeidDokumentasjonsKravTjeneste;
    private KontaktInformasjonTjeneste kontaktInformasjonTjeneste;

    @Inject
    public ArbeidRest(AnnenPartSakTjeneste annenPartSakTjeneste, TilgangKontrollTjeneste tilgangkontroll, InnloggetBruker innloggetBruker,
                      PersonOppslagSystem personOppslagSystem, MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste,
                      AktivitetskravArbeidDokumentasjonsKravTjeneste aktivitetskravArbeidDokumentasjonsKravTjeneste,
                      KontaktInformasjonTjeneste kontaktInformasjonTjeneste) {
        this.annenPartSakTjeneste = annenPartSakTjeneste;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
        this.personOppslagSystem = personOppslagSystem;
        this.mineArbeidsforholdTjeneste = mineArbeidsforholdTjeneste;
        this.aktivitetskravArbeidDokumentasjonsKravTjeneste = aktivitetskravArbeidDokumentasjonsKravTjeneste;
        this.kontaktInformasjonTjeneste = kontaktInformasjonTjeneste;
    }

    ArbeidRest() {
    }

    @Path("/mineArbeidsforhold")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<EksternArbeidsforholdDto> hentMittArbeid() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();

        return mineArbeidsforholdTjeneste.brukersArbeidsforhold(innloggetBruker.fødselsnummer());
    }

    @Path("/mineFrilansoppdrag")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<EksternArbeidsforholdDto> hentMineFrilansoppdrag() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();

        return mineArbeidsforholdTjeneste.brukersFrilansoppdragSisteSeksMåneder(innloggetBruker.fødselsnummer());
    }

    @Path("/morDokumentasjon")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public boolean trengerDokumentereMorsArbeid(@Valid @NotNull ArbeidRest.MorArbeidRequest request) {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        try {
            var adresseBeskyttelse = personOppslagSystem.adresseBeskyttelse(request.annenPartFødselsnummer());
            if (adresseBeskyttelse.harBeskyttetAdresse()) {
                return true;
            }
        } catch (BrukerIkkeFunnetIPdlException e) {
            LOG.info("Klarer ikke å finne adressebeskyttelse for annen part, person ikke funnet i pdl. Søker må derfor dokumentere mors arbeid", e);
            return true;
        }
        if (kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(request.annenPartFødselsnummer())) {
            LOG.info("Mor er reservert eller kan ikke varsles. Søker må derfor dokumentere mors arbeid.");
            return true;
        }

        LOG.debug("Kall mot morsArbeidDokumentasjon-endepunkt");
        var søkerAktørId = innloggetBruker.aktørId();
        var annenPartAktørId = personOppslagSystem.aktørId(request.annenPartFødselsnummer());
        var barnAktørId = request.barnFødselsnummer() == null ? null : personOppslagSystem.aktørId(request.barnFødselsnummer());
        var familieHendelse = request.familiehendelse();
        var annenPartSak = annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(søkerAktørId, annenPartAktørId, barnAktørId, familieHendelse);

        // Tilfelle bare far rett - der kan vi ikke vente at mor har en sak. Dersom mor har engangsstønad så har hun ikke oppgitt far/medmor
        if (annenPartSak.isEmpty()) {
            if (request.barnFødselsnummer() == null) {
                return true;
            }
            if (!personOppslagSystem.barnHarDisseForeldrene(request.barnFødselsnummer(), request.annenPartFødselsnummer(), innloggetBruker.fødselsnummer())) {
                return true;
            }
        }
        var intervaller = request.perioder().stream()
            .map(p -> new LocalDateInterval(p.fom(), p.tom()))
            .toList();
        var aktivitetskravrequest = new PerioderMedAktivitetskravArbeid(request.annenPartFødselsnummer(), intervaller);
        return aktivitetskravArbeidDokumentasjonsKravTjeneste.krevesDokumentasjonForAktivitetskravArbeid(aktivitetskravrequest);
    }

    public record MorArbeidRequest(@Valid @NotNull Fødselsnummer annenPartFødselsnummer, @Valid Fødselsnummer barnFødselsnummer,
                                   LocalDate familiehendelse, @Valid @Size(min = 1) List<@Valid PeriodeRequest> perioder) {
    }

    public record PeriodeRequest(@Valid @NotNull LocalDate fom, @Valid @NotNull LocalDate tom) {}
}
