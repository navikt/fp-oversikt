package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.Set;
import java.util.stream.Collectors;

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
                oppgaveRepository.oppdaterStatus(o.id(), OppgaveStatus.AVSLUTTET_DITT_NAV);
            }
        }
    }

    private void opprettOppgaver(Saksnummer saksnummer, Fødselsnummer fnr) {
        var oppgaverSomMåOpprettes = finnOppgaverSomMåOpprettes(saksnummer);
        if (!oppgaverSomMåOpprettes.isEmpty()) {
            dittNav.opprett(fnr, saksnummer, oppgaverSomMåOpprettes);
            for (var o : oppgaverSomMåOpprettes) {
                oppgaveRepository.oppdaterStatus(o.id(), OppgaveStatus.OPPRETTET_DITT_NAV);
            }
        }
    }

    private Set<Oppgave> finnOppgaverSomMåOpprettes(Saksnummer saksnummer) {
        return hentOppgaverMedStatus(saksnummer, OppgaveStatus.OPPRETTET);
    }

    private Set<Oppgave> finnOppgaverSomMåAvsluttes(Saksnummer saksnummer) {
        return hentOppgaverMedStatus(saksnummer, OppgaveStatus.AVSLUTTET);
    }

    private Set<Oppgave> hentOppgaverMedStatus(Saksnummer saksnummer, OppgaveStatus status) {
        return oppgaveRepository.hentFor(saksnummer).stream().filter(o -> o.status() == status).collect(Collectors.toSet());
    }

    private Fødselsnummer finnFødselsnummer(Saksnummer saksnummer) {
        var aktørId = sakRepository.hentFor(saksnummer).aktørId();
        return pdlKlient.hentFnrFor(aktørId);
    }
}
