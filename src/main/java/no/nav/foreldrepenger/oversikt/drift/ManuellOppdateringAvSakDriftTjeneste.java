package no.nav.foreldrepenger.oversikt.drift;


import static no.nav.foreldrepenger.common.domain.validation.InputValideringRegex.BARE_TALL;
import static no.nav.foreldrepenger.oversikt.drift.ProsessTaskRestTjeneste.sjekkAtSaksbehandlerHarRollenDrift;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.foreldrepenger.oversikt.innhenting.HentSakTask;
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
    public Response opprettHentSakTaskForSaksnummre(@Parameter(description = "Liste med saksnummre som skal oppdateres") @NotNull List<@Valid @NotNull @Pattern(regexp = BARE_TALL) String> saksnummre) {
        sjekkAtSaksbehandlerHarRollenDrift();
        for (var saksnummer : saksnummre) {
            LOG.info("Lager task for å oppdatere følgende sak {}", saksnummer);
            lagreHentSakTask(saksnummer);
        }
        return Response.ok().build();
    }

    private void lagreHentSakTask(String saksnummer) {
        var task = opprettTask(saksnummer);
        taskTjeneste.lagre(task);
    }

    public static ProsessTaskData opprettTask(String saksnummer) {
        var task = ProsessTaskData.forProsessTask(HentSakTask.class);
        task.setProperty(HentSakTask.SAKSNUMMER, saksnummer);
        task.setPrioritet(50);
        task.medNesteKjøringEtter(LocalDateTime.now());
        task.setCallIdFraEksisterende();
        task.setGruppe(saksnummer);
        task.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        return task;
    }

}
