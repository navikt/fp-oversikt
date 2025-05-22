package no.nav.foreldrepenger.oversikt.arbeid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.fpsak.tidsserie.LocalDateSegment;

@ApplicationScoped
public class AktivitetskravMåDokumentereMorsArbeidTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AktivitetskravMåDokumentereMorsArbeidTjeneste.class);

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
        // Tilfelle bare far rett - der kan vi ikke vente at mor har en sak. Dersom mor har engangsstønad så har hun ikke oppgitt far/medmor
        if (annenpartHarIkkeSakMedOppgittBarnOgSøker(søkersAktørid, request)) {
            if (request.barnFødselsnummer() == null) {
                LOG.info("AKTIVTETSKRAV: Annenpart har ikke sak knyttet til søker/barn og vi kan ikke slå opp barnet for å sjekke relasjon");
                return true;
            }
            if (!personOppslagSystem.barnHarDisseForeldrene(request.barnFødselsnummer(), request.annenPartFødselsnummer(), søkersFnr)) {
                LOG.info("AKTIVTETSKRAV: Annenpart har ikke sak som gjelder både søker og barn, men barn har ikke relasjon til en eller begge av forelderene");
                return true;
            }
            LOG.info("AKTIVTETSKRAV: Annenpart har ikke sak men barnet er relatert til både søker og annenpart");
        }

        if (kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(request.annenPartFødselsnummer())) {
            LOG.info("AKTIVTETSKRAV: Mor er reservert eller kan ikke varsles. Søker må derfor dokumentere mors arbeid.");
            return true;
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
        var segmenter = request.perioder().stream()
            .map(AktivitetskravMåDokumentereMorsArbeidTjeneste::mapPeriode)
            .toList();
        var aktivitetskravrequest = new PerioderMedAktivitetskravArbeid(request.annenPartFødselsnummer(), segmenter);
        return aktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste.krevesDokumentasjonForAktivitetskravArbeid(aktivitetskravrequest);
    }

    private static LocalDateSegment<PeriodeMedAktivitetskravType> mapPeriode(ArbeidRest.PeriodeRequest periode) {
        return new LocalDateSegment<>(periode.fom(), periode.tom(),
            periode.periodeType() != null ? periode.periodeType() : PeriodeMedAktivitetskravType.UTTAK);
    }

    private boolean annenpartHarIkkeSakMedOppgittBarnOgSøker(AktørId innloggetBrukerAktørid, ArbeidRest.MorArbeidRequest request) {
        var annenPartAktørId = personOppslagSystem.aktørId(request.annenPartFødselsnummer());
        var barnAktørId = request.barnFødselsnummer() == null ? null : personOppslagSystem.aktørId(request.barnFødselsnummer());
        var familieHendelse = request.familiehendelse();
        return annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(innloggetBrukerAktørid, annenPartAktørId, barnAktørId, familieHendelse).isEmpty();
    }
}
