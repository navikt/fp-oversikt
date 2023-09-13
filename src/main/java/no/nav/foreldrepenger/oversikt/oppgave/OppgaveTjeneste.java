package no.nav.foreldrepenger.oversikt.oppgave;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class OppgaveTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private static final Environment ENV = Environment.current();

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private OppgaveRepository oppgaveRepository;

    @Inject
    public OppgaveTjeneste(ProsessTaskTjeneste prosessTaskTjeneste, OppgaveRepository oppgaveRepository) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.oppgaveRepository = oppgaveRepository;
    }

    OppgaveTjeneste() {
    }

    public void oppdaterOppgaver(Saksnummer saksnummer, Set<OppgaveType> nyeOppgaver) {
        LOG.info("Oppdaterer oppgaver for {} {}", saksnummer, nyeOppgaver);
        if (ENV.isProd()) {
            LOG.info("Oppgaver er disabled");
            return;
        }

        var eksisterendeOppgaver = hentAktiveOppgaver(saksnummer);
        LOG.info("Hentet eksisterende oppgaver for {} {}", saksnummer, eksisterendeOppgaver);
        var eksisterendeOppgaveTyper = eksisterendeOppgaver.stream().map(Oppgave::type).collect(Collectors.toSet());
        var oppgaverSomSkalAvsluttes = eksisterendeOppgaver.stream()
            .filter(oppgave -> !nyeOppgaver.contains(oppgave.type()))
            .collect(Collectors.toSet());
        var oppgaverSomSkalOpprettes = nyeOppgaver.stream()
            .filter(o -> !eksisterendeOppgaveTyper.contains(o))
            .map(o -> new Oppgave(saksnummer, UUID.randomUUID(), o, new Oppgave.Status(LocalDateTime.now(), null), null))
            .collect(Collectors.toSet());

        oppdaterDb(oppgaverSomSkalOpprettes, oppgaverSomSkalAvsluttes);
        opprettOppdaterDittNavTask(saksnummer);
    }

    private Set<Oppgave> hentAktiveOppgaver(Saksnummer saksnummer) {
        return oppgaveRepository.hentFor(saksnummer).stream().filter(Oppgave::aktiv).collect(Collectors.toSet());
    }

    private void oppdaterDb(Set<Oppgave> oppgaverSomSkalOpprettes, Set<Oppgave> oppgaverSomSkalAvsluttes) {
        for (var o : oppgaverSomSkalAvsluttes) {
            LOG.info("Avslutter oppgave {}", o);
            oppgaveRepository.lagreStatus(o.id(), new Oppgave.Status(o.status().opprettetTidspunkt(), LocalDateTime.now()));
        }
        LOG.info("Oppretter oppgaver {}", oppgaverSomSkalOpprettes);
        oppgaveRepository.opprett(oppgaverSomSkalOpprettes);
    }

    private void opprettOppdaterDittNavTask(Saksnummer saksnummer) {
        var task = ProsessTaskData.forProsessTask(OppdaterDittNavTask.class);
        task.setCallIdFraEksisterende();
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer.value() + "-dittnav");
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        prosessTaskTjeneste.lagre(task);
    }

    public void opprettOppdaterOppgaveTask(Saksnummer saksnummer) {
        var task = ProsessTaskData.forProsessTask(OpprettOppgaverTask.class);
        task.setCallIdFraEksisterende();
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer.value() + "-oppgave");
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        prosessTaskTjeneste.lagre(task);
    }
}
