package no.nav.foreldrepenger.oversikt.saker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpostSelvbetjening;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostType;
import no.nav.foreldrepenger.oversikt.arkiv.SafSelvbetjeningTjeneste;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Comparator;

import static java.util.stream.Stream.concat;

@Path("/saker")
@ApplicationScoped
@Transactional
public class SakerRest {
    private static final Logger LOG = LoggerFactory.getLogger(SakerRest.class);
    private static final String TITTEL_VED_SØKNAD = "Søknad om";

    private Saker saker;
    private SafSelvbetjeningTjeneste safSelvbetjeningTjeneste;
    private InnloggetBruker innloggetBruker;
    private TilgangKontrollTjeneste tilgangkontroll;

    @Inject
    public SakerRest(Saker saker, SafSelvbetjeningTjeneste safSelvbetjeningTjeneste, InnloggetBruker innloggetBruker, TilgangKontrollTjeneste tilgangkontroll) {
        this.saker = saker;
        this.safSelvbetjeningTjeneste = safSelvbetjeningTjeneste;
        this.innloggetBruker = innloggetBruker;
        this.tilgangkontroll = tilgangkontroll;
    }

    SakerRest() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public no.nav.foreldrepenger.common.innsyn.Saker hent() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        LOG.debug("Kall mot saker endepunkt");
        return saker.hent();
    }

    @GET
    @Path("/erOppdatert")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean erSakOppdatertEtterMottattSøknad() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        var dokumenter = safSelvbetjeningTjeneste.alleJournalposter(innloggetBruker.fødselsnummer());
        var søkaderMottattNylig = dokumenter.stream()
                .filter(arkivDokument -> JournalpostType.INNGÅENDE_DOKUMENT.equals(arkivDokument.type()))
                .filter(arkivDokument -> arkivDokument.tittel().contains(TITTEL_VED_SØKNAD))
                .filter(arkivDokument -> arkivDokument.mottatt().isAfter(LocalDateTime.now().minusDays(1)))
                .toList();

        if (søkaderMottattNylig.isEmpty()) {
            return true;
        }

        if (søkaderMottattNylig.stream().anyMatch(søknad -> søknad.saksnummer() == null)) {
            var førsteMottattDato = søkaderMottattNylig.stream()
                    .min(Comparator.comparing(EnkelJournalpostSelvbetjening::mottatt))
                    .map(EnkelJournalpostSelvbetjening::mottatt)
                    .orElse(null);
            LOG.info("Sak ikke oppdatert. Fant søknad hvor saksnummer er null -> GOSYS. Antall {} Mottatt {}", søkaderMottattNylig.size(), førsteMottattDato);
            return false;
        }

        // Vi har nylig mottatt dokument. Sjekk at saken er oppdatert etter dette tidspunktet.
        var sakerFraFpoversikt = saker.hent();
        var listeMedSakerFraFpoversikt = concat(sakerFraFpoversikt.foreldrepenger().stream(),
                concat(sakerFraFpoversikt.svangerskapspenger().stream(), sakerFraFpoversikt.engangsstønad().stream()))
                .toList();

        for (var søknad : søkaderMottattNylig) {
            var erSakOppdatert = listeMedSakerFraFpoversikt.stream()
                    .filter(sak -> sak.saksnummer().value().equals(søknad.saksnummer()))
                    .anyMatch(sak -> sak.oppdatertTidspunkt().isAfter(søknad.mottatt()));
            if (!erSakOppdatert) {
                LOG.info("Sak ikke oppdatert. Gjelder sak {} Mottatt {}", søknad.saksnummer(), søknad.mottatt());
                return false;
            }
        }
        return true;
    }
}
