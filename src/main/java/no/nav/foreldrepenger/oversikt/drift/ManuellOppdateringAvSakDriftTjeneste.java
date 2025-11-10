package no.nav.foreldrepenger.oversikt.drift;


import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.HentSakTask;
import no.nav.foreldrepenger.oversikt.oppgave.OpprettOppgaverTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@OpenAPIDefinition(tags = @Tag(name = "saker", description = "Manuell oppdatering av saker"))
@Path("/forvaltningSaker")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@Transactional
public class ManuellOppdateringAvSakDriftTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ManuellOppdateringAvSakDriftTjeneste.class);

    private ProsessTaskTjeneste taskTjeneste;

    ManuellOppdateringAvSakDriftTjeneste() {
        // REST CDI
    }

    @Inject
    public ManuellOppdateringAvSakDriftTjeneste(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    @POST
    @Path("/oppdater")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter prosesstask for manuelt oppdatering av saker", tags = "saker", responses = {
        @ApiResponse(responseCode = "200", description = "HentSakTask opprettet for alle saknsummre"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    public Response opprettHentSakTaskForSaksnummre(@Parameter(description = "Liste med saksnummre som skal oppdateres") @Valid @NotNull List<@Valid @NotNull Saksnummer> saksnummre) {
        for (var saksnummer : saksnummre) {
            LOG.info("Lager task for å oppdatere følgende sak {}", saksnummer.value());
            lagreHentSakTask(saksnummer);
        }
        return Response.ok().build();
    }

    @POST
    @Path("/oppdaterOppgaver")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Utleder og oppretter oppgaver på sak", tags = "saker", responses = {
        @ApiResponse(responseCode = "200", description = "Task for å oppdatere oppgaver er opprettet per sak")
    })
    public Response oppdaterOppgaver(@Parameter(description = "Liste med saksnummre som skal oppdateres") @Valid @NotNull List<@Valid @NotNull Saksnummer> saksnummre) {
        for (var saksnummer : saksnummre) {
            LOG.info("Lager task for å oppdatere oppgaver {}", saksnummer.value());
            lagreOpprettOppgaveTask(saksnummer);
        }
        return Response.ok().build();
    }

    private void lagreOpprettOppgaveTask(Saksnummer saksnummer) {
        var task = opprettOpprettOppgaveTask(saksnummer);
        taskTjeneste.lagre(task);
    }

    private static ProsessTaskData opprettOpprettOppgaveTask(Saksnummer saksnummer) {
        var task = ProsessTaskData.forProsessTask(OpprettOppgaverTask.class);
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer.value() + "-oppgave");
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

    private void lagreHentSakTask(Saksnummer saksnummer) {
        var task = opprettTask(saksnummer);
        taskTjeneste.lagre(task);
    }

    private static ProsessTaskData opprettTask(Saksnummer saksnummer) {
        var task = ProsessTaskData.forProsessTask(HentSakTask.class);
        task.setSaksnummer(saksnummer.value());
        task.setGruppe(saksnummer.value());
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

}
