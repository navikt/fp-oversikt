package no.nav.foreldrepenger.oversikt.arbeid;


import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.oppslag.MineArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@Path("/arbeid")
@ApplicationScoped
@Transactional
public class ArbeidRest {

    private static final Logger LOG = LoggerFactory.getLogger(ArbeidRest.class);

    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;
    private PersonOppslagSystem personOppslagSystem;
    private MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste;
    private AktivitetskravMåDokumentereMorsArbeidTjeneste aktivitetskravMåDokumentereMorsArbeidTjeneste;

    @Inject
    public ArbeidRest(TilgangKontrollTjeneste tilgangkontroll, InnloggetBruker innloggetBruker,
                      PersonOppslagSystem personOppslagSystem, MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste,
                      AktivitetskravMåDokumentereMorsArbeidTjeneste aktivitetskravMåDokumentereMorsArbeidTjeneste) {
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
        this.personOppslagSystem = personOppslagSystem;
        this.mineArbeidsforholdTjeneste = mineArbeidsforholdTjeneste;
        this.aktivitetskravMåDokumentereMorsArbeidTjeneste = aktivitetskravMåDokumentereMorsArbeidTjeneste;
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
        return aktivitetskravMåDokumentereMorsArbeidTjeneste.krevesDokumentasjonForAktivitetskravArbeid(innloggetBruker.fødselsnummer(), innloggetBruker.aktørId(), request);
    }

    public record MorArbeidRequest(@Valid @NotNull Fødselsnummer annenPartFødselsnummer, @Valid Fødselsnummer barnFødselsnummer,
                                   LocalDate familiehendelse, @Valid @Size(min = 1) List<@Valid PeriodeRequest> perioder) {
    }

    public record PeriodeRequest(@Valid @NotNull LocalDate fom, @Valid @NotNull LocalDate tom, @Valid PeriodeMedAktivitetskravType periodeType) {}

}
