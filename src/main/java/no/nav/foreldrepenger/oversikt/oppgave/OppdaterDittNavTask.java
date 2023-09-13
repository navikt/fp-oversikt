package no.nav.foreldrepenger.oversikt.oppgave;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.PdlKlientSystem;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "oppdater.dittnav")
class OppdaterDittNavTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OppdaterDittNavTask.class);

    private final PdlKlientSystem pdlKlient;
    private final SakRepository sakRepository;
    private final DittNav dittNav;
    private final OppgaveRepository oppgaveRepository;

    @Inject
    OppdaterDittNavTask(PdlKlientSystem pdlKlient, SakRepository sakRepository, DittNav dittNav, OppgaveRepository oppgaveRepository) {
        this.pdlKlient = pdlKlient;
        this.sakRepository = sakRepository;
        this.dittNav = dittNav;
        this.oppgaveRepository = oppgaveRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());
        var fnr = finnFødselsnummer(saksnummer);

        avsluttOppgaver(saksnummer, fnr);
        opprettOppgaver(saksnummer, fnr);
    }

    private void avsluttOppgaver(Saksnummer saksnummer, Fødselsnummer fnr) {
        var oppgaverSomMåAvsluttes = finnOppgaverSomMåAvsluttes(saksnummer);
        if (!oppgaverSomMåAvsluttes.isEmpty()) {
            dittNav.avslutt(fnr, saksnummer, oppgaverSomMåAvsluttes);
            for (var o : oppgaverSomMåAvsluttes) {
                var nyStatus = new Oppgave.StatusDittNav(o.dittNavStatus().opprettetTidspunkt(), LocalDateTime.now());
                LOG.info("Avslutter dittnav oppave {} - {}", o, nyStatus);
                oppgaveRepository.lagreStatusDittNav(o.id(), nyStatus);
            }
        }
    }

    private void opprettOppgaver(Saksnummer saksnummer, Fødselsnummer fnr) {
        var oppgaverSomMåOpprettes = finnOppgaverSomMåOpprettes(saksnummer);
        if (!oppgaverSomMåOpprettes.isEmpty()) {
            dittNav.opprett(fnr, saksnummer, oppgaverSomMåOpprettes);
            for (var o : oppgaverSomMåOpprettes) {
                var nyStatus = new Oppgave.StatusDittNav(LocalDateTime.now(), null);
                LOG.info("Oppretter dittnav oppave {} - {}", o, nyStatus);
                oppgaveRepository.lagreStatusDittNav(o.id(), nyStatus);
            }
        }
    }

    private Set<Oppgave> finnOppgaverSomMåOpprettes(Saksnummer saksnummer) {
        return oppgaveRepository.hentFor(saksnummer)
            .stream()
            .filter(o -> o.status().opprettetTidspunkt() != null && o.dittNavStatus().opprettetTidspunkt() == null && o.status().avsluttetTidspunkt() == null)
            .collect(Collectors.toSet());
    }

    private Set<Oppgave> finnOppgaverSomMåAvsluttes(Saksnummer saksnummer) {
        return oppgaveRepository.hentFor(saksnummer)
            .stream()
            .filter(o -> o.status().avsluttetTidspunkt() != null && o.dittNavStatus().avsluttetTidspunkt() == null)
            .collect(Collectors.toSet());
    }

    private Fødselsnummer finnFødselsnummer(Saksnummer saksnummer) {
        var aktørId = sakRepository.hentFor(saksnummer).aktørId();
        return pdlKlient.hentFnrFor(aktørId);
    }
}
