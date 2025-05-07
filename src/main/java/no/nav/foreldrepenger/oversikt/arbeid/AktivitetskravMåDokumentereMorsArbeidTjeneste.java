package no.nav.foreldrepenger.oversikt.arbeid;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class AktivitetskravMåDokumentereMorsArbeidTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AktivitetskravMåDokumentereMorsArbeidTjeneste.class);
    private static final Environment ENV = Environment.current();

    private PersonOppslagSystem personOppslagSystem;
    private AnnenPartSakTjeneste annenPartSakTjeneste;
    private KontaktInformasjonTjeneste kontaktInformasjonTjeneste;
    private AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste;

    public AktivitetskravMåDokumentereMorsArbeidTjeneste() {
        // CDI
    }

    @Inject
    public AktivitetskravMåDokumentereMorsArbeidTjeneste(PersonOppslagSystem personOppslagSystem,
                                                         AnnenPartSakTjeneste annenPartSakTjeneste,
                                                         KontaktInformasjonTjeneste kontaktInformasjonTjeneste,
                                                         AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste) {
        this.personOppslagSystem = personOppslagSystem;
        this.annenPartSakTjeneste = annenPartSakTjeneste;
        this.kontaktInformasjonTjeneste = kontaktInformasjonTjeneste;
        this.aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste = aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste;
    }

    public boolean krevesDokumentasjonForAktivitetskravArbeid(Fødselsnummer søkersFnr, AktørId søkersAktørid, ArbeidRest.MorArbeidRequest request) {
        if (ENV.isProd()) {
            return true; //TODO TFP-5383
        }
        if (kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(request.annenPartFødselsnummer())) {
            LOG.info("AKTIVTETSKRAV: Mor er reservert eller kan ikke varsles. Søker må derfor dokumentere mors arbeid.");
            return true;
        }

        // Tilfelle bare far rett - der kan vi ikke vente at mor har en sak. Dersom mor har engangsstønad så har hun ikke oppgitt far/medmor
        if (annenpartSomGjelderBarnHvorSøkerErOppgitt(søkersAktørid, request).isEmpty()) {
            LOG.info("AKTIVTETSKRAV: Annenpart har ikke sak som gjelder søker og barn");
            if (request.barnFødselsnummer() == null) {
                LOG.info("AKTIVTETSKRAV: Annenpart har ikke sak som gjelder søker og barn. og fødselsnummer på barn er ikke oppgitt");
                return true;
            }
            if (!personOppslagSystem.barnHarDisseForeldrene(request.barnFødselsnummer(), request.annenPartFødselsnummer(), søkersFnr)) {
                LOG.info("AKTIVTETSKRAV: Annenpart har ikke sak som gjelder både søker og barn, men barn har ikke relasjon til en eller begge av forelderene");
                return true;
            }
        }
        var krevesDokumentasjonForAktivitetskravArbeid = krevesDokumentasjonForAktivitetskravArbeid(request);
        if (krevesDokumentasjonForAktivitetskravArbeid) {
            LOG.info("AKTIVTETSKRAV: Mor har ikke arbeidsforhold som dekker aktivitetskravet for oppgitte perioder");
        } else {
            LOG.info("AKTIVTETSKRAV: Mor har arbeidsforhold som dekker aktivitetskravet for oppgitte perioder");
        }
        return krevesDokumentasjonForAktivitetskravArbeid;
    }

    private boolean krevesDokumentasjonForAktivitetskravArbeid(ArbeidRest.MorArbeidRequest request) {
        var intervaller = request.perioder().stream()
                .map(p -> new LocalDateInterval(p.fom(), p.tom()))
                .toList();
        var aktivitetskravrequest = new PerioderMedAktivitetskravArbeid(request.annenPartFødselsnummer(), intervaller);
        return aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste.krevesDokumentasjonForAktivitetskravArbeid(aktivitetskravrequest);
    }

    private Optional<ForeldrepengerSak> annenpartSomGjelderBarnHvorSøkerErOppgitt(AktørId innloggetBrukerAktørid, ArbeidRest.MorArbeidRequest request) {
        var annenPartAktørId = personOppslagSystem.aktørId(request.annenPartFødselsnummer());
        var barnAktørId = request.barnFødselsnummer() == null ? null : personOppslagSystem.aktørId(request.barnFødselsnummer());
        var familieHendelse = request.familiehendelse();
        return annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(innloggetBrukerAktørid, annenPartAktørId, barnAktørId, familieHendelse);
    }
}
