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
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class OppgaveTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveTjeneste.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private OppgaveRepository oppgaveRepository;

    @Inject
    public OppgaveTjeneste(ProsessTaskTjeneste prosessTaskTjeneste, OppgaveRepository oppgaveRepository) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.oppgaveRepository = oppgaveRepository;
    }

    OppgaveTjeneste() {
    }

    public void oppdaterOppgaver(Saksnummer saksnummer, Set<OppgaveType> oppgaver) {
        LOG.info("Oppdaterer oppgaver for {} {}", saksnummer.value(), oppgaver);

        var eksisterendeOppgaver = hentAktiveOppgaver(saksnummer);
        LOG.info("Hentet eksisterende oppgaver for {} {}", saksnummer.value(), eksisterendeOppgaver);
        avsluttOppgaver(oppgaver, eksisterendeOppgaver);
        opprettOppgaver(saksnummer, oppgaver, eksisterendeOppgaver);
    }

    private void opprettOppgaver(Saksnummer saksnummer, Set<OppgaveType> oppgaver, Set<Oppgave> eksisterendeOppgaver) {
        var eksisterendeOppgaveTyper = eksisterendeOppgaver.stream().map(Oppgave::type).collect(Collectors.toSet());
        var oppgaverSomSkalOpprettes = oppgaver.stream()
            .filter(o -> !eksisterendeOppgaveTyper.contains(o))
            .map(o -> new Oppgave(saksnummer, UUID.randomUUID(), o, new Oppgave.Status(LocalDateTime.now(), null)))
            .collect(Collectors.toSet());
        for (var o : oppgaverSomSkalOpprettes) {
            LOG.info("Oppretter oppgave {}", o);
            oppgaveRepository.opprett(o);
            opprettOpprettDittNavOppgaveTask(o.id());
        }
    }

    private void avsluttOppgaver(Set<OppgaveType> oppgaver, Set<Oppgave> eksisterendeOppgaver) {
        var oppgaverSomSkalAvsluttes = eksisterendeOppgaver.stream()
            .filter(oppgave -> !oppgaver.contains(oppgave.type()))
            .collect(Collectors.toSet());
        for (var o : oppgaverSomSkalAvsluttes) {
            LOG.info("Avslutter oppgave {}", o);
            oppgaveRepository.lagreStatus(o.id(), new Oppgave.Status(o.status().opprettetTidspunkt(), LocalDateTime.now()));
            opprettAvsluttDittNavOppgaveTask(o.id());
        }
    }

    private Set<Oppgave> hentAktiveOppgaver(Saksnummer saksnummer) {
        return oppgaveRepository.hentFor(saksnummer).stream().filter(Oppgave::aktiv).collect(Collectors.toSet());
    }

    private void opprettOpprettDittNavOppgaveTask(UUID id) {
        var task = ProsessTaskData.forProsessTask(MinSideOpprettOppgaveTask.class);
        task.setCallIdFraEksisterende();
        task.setProperty(MinSideOpprettOppgaveTask.OPPGAVE_ID, id.toString());
        task.setGruppe(id.toString());
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        prosessTaskTjeneste.lagre(task);
    }

    private void opprettAvsluttDittNavOppgaveTask(UUID id) {
        var task = ProsessTaskData.forProsessTask(MinSideAvsluttOppgaveTask.class);
        task.setCallIdFraEksisterende();
        task.setProperty(MinSideAvsluttOppgaveTask.OPPGAVE_ID, id.toString());
        task.setGruppe(id.toString());
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
