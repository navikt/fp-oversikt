package no.nav.foreldrepenger.oversikt.saker;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;
import no.nav.foreldrepenger.common.innsyn.Dekningsgrad;
import no.nav.foreldrepenger.common.innsyn.Familiehendelse;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.FpVedtak;
import no.nav.foreldrepenger.common.innsyn.FpÅpenBehandling;
import no.nav.foreldrepenger.common.innsyn.KontoType;
import no.nav.foreldrepenger.common.innsyn.RettighetType;
import no.nav.foreldrepenger.common.innsyn.Saker;
import no.nav.foreldrepenger.common.innsyn.Saksnummer;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.common.innsyn.UttakPeriodeResultat;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@Path("/saker")
@ApplicationScoped
@Transactional
public class SakerRest {

    private static final Logger LOG = LoggerFactory.getLogger(SakerRest.class);
    private FpSaker fpSaker;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public SakerRest(FpSaker fpSaker, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.fpSaker = fpSaker;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    SakerRest() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Saker hent() {
        opprettTestTask();
        var uid = KontekstHolder.getKontekst().getUid();
        LOG.info("Kall mot saker endepunkt");
        var fpSakerForBruker = fpSaker.hent();
        return tilDto(fpSakerForBruker);
    }

    private void opprettTestTask() {
        var task = ProsessTaskData.forProsessTask(TestTask.class);
        task.setPrioritet(50);
        task.setCallIdFraEksisterende();
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task");
    }

    private Saker tilDto(Object fpSakerForBruker) {
        var uttakPeriode = new UttakPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1), KontoType.MØDREKVOTE,
            new UttakPeriodeResultat(true, false, true, UttakPeriodeResultat.Årsak.ANNET), null, null, null, null, null, null, false);
        var sak = new FpSak(new Saksnummer("1"), false, LocalDate.now(), false, true, false, false, false, false, RettighetType.BEGGE_RETT, null,
            new Familiehendelse(null, LocalDate.now(), 1, null), new FpVedtak(List.of(uttakPeriode)),
            new FpÅpenBehandling(BehandlingTilstand.VENT_DOKUMENTASJON, List.of(uttakPeriode)), Set.of(), Dekningsgrad.HUNDRE);
        return new Saker(Set.of(sak), Set.of(), Set.of());
    }
}
