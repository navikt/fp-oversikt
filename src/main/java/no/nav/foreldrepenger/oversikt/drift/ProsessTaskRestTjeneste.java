package no.nav.foreldrepenger.oversikt.drift;

import java.net.HttpURLConnection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.rest.app.ProsessTaskApplikasjonTjeneste;
import no.nav.vedtak.felles.prosesstask.rest.dto.FeiletProsessTaskDataDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.IkkeFerdigProsessTaskStatusEnum;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskDataDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskIdDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskOpprettInputDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskRestartInputDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskRestartResultatDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskRetryAllResultatDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskSetFerdigInputDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.SokeFilterDto;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

@OpenAPIDefinition(tags = @Tag(name = "prosesstask", description = "Håndtering av asynkrone oppgaver i form av prosesstask"))
@Path("/prosesstask")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@Transactional
public class ProsessTaskRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ProsessTaskRestTjeneste.class);

    private ProsessTaskApplikasjonTjeneste prosessTaskApplikasjonTjeneste;

    ProsessTaskRestTjeneste() {
        // REST CDI
    }

    @Inject
    public ProsessTaskRestTjeneste(ProsessTaskApplikasjonTjeneste prosessTaskApplikasjonTjeneste) {
        this.prosessTaskApplikasjonTjeneste = prosessTaskApplikasjonTjeneste;
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en prosess task i henhold til request", summary = "Oppretter en ny task klar for kjøring.", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "202", description = "Prosesstaskens oppdatert informasjon", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskDataDto.class))),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    public ProsessTaskDataDto createProsessTask(@Parameter(description = "Informasjon for restart en eksisterende prosesstask") @Valid ProsessTaskOpprettInputDto inputDto) {
        sjekkAtSaksbehandlerHarRollenDrift();
        // kjøres manuelt for å avhjelpe feilsituasjon, da er det veldig greit at det blir logget!
        LOG.info("Oppretter prossess task av type {}", inputDto.getTaskType());
        return prosessTaskApplikasjonTjeneste.opprettTask(inputDto);
    }

    @POST
    @Path("/launch/{prosessTaskId}/{prosessTaskStatus}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Restarter en eksisterende prosesstask.", summary = "En allerede FERDIG prosesstask kan ikke restartes. En prosesstask har normalt et gitt antall forsøk den kan kjøres automatisk. "
        +
        "Dette endepunktet vil tvinge tasken til å trigge uavhengig av maks antall forsøk", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Prosesstaskens oppdatert informasjon", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskRestartResultatDto.class))),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    public ProsessTaskRestartResultatDto restartProsessTask(@Parameter(description = "Informasjon for restart en eksisterende prosesstask") @Valid @BeanParam ProsessTaskRestartInputDto restartInputDto) {
        sjekkAtSaksbehandlerHarRollenDrift();
        // kjøres manuelt for å avhjelpe feilsituasjon, da er det veldig greit at det blir logget!
        LOG.info("Restarter prossess task {}", restartInputDto.getProsessTaskId());
        return prosessTaskApplikasjonTjeneste.flaggProsessTaskForRestart(restartInputDto.getProsessTaskId(), ProsessTaskStatus.valueOf(restartInputDto.getNaaVaaerendeStatus().name()));
    }

    @POST
    @Path("/retryall")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Restarter alle prosesstask med status FEILET.", summary = "Dette endepunktet vil tvinge feilede tasks til å trigge ett forsøk uavhengig av maks antall forsøk", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Response med liste av prosesstasks som restartes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskRetryAllResultatDto.class))),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    public ProsessTaskRetryAllResultatDto retryAllProsessTask() {
        sjekkAtSaksbehandlerHarRollenDrift();
        // kjøres manuelt for å avhjelpe feilsituasjon, da er det veldig greit at det blir logget!
        LOG.info("Restarter alle prossess task i status FEILET");
        return prosessTaskApplikasjonTjeneste.flaggAlleFeileteProsessTasksForRestart();
    }

    @POST
    @Path("/list/{prosessTaskStatus}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lister prosesstasker med angitt status.", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Liste over prosesstasker, eller tom liste når angitt/default søkefilter ikke finner noen prosesstasker", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskDataDto.class)))
    })
    public List<ProsessTaskDataDto> finnProsessTasks(@Parameter(description = "Task status som skal hentes.") @Valid @PathParam("prosessTaskStatus") IkkeFerdigProsessTaskStatusEnum finnTaskStatus) {
        sjekkAtSaksbehandlerHarRollenDrift();
        var statusFilter = List.of(ProsessTaskStatus.valueOf(finnTaskStatus.name()));
        return prosessTaskApplikasjonTjeneste.finnAlle(statusFilter);
    }

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Søker etter prosesstask med angitt tekst i properties.", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Liste over prosesstasker, eller tom liste når angitt/default søkefilter ikke finner noen prosesstasker", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskDataDto.class)))
    })
    public List<ProsessTaskDataDto> searchProsessTasks(@Parameter(description = "Søkefilter for å begrense resultatet av returnerte prosesstask.") @Valid SokeFilterDto sokeFilterDto) {
        sjekkAtSaksbehandlerHarRollenDrift();
        return prosessTaskApplikasjonTjeneste.søk(sokeFilterDto);
    }

    @POST
    @Path("/feil/{prosessTaskId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter informasjon om feilet prosesstask med angitt prosesstask-id", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Angitt prosesstask-id finnes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeiletProsessTaskDataDto.class))),
        @ApiResponse(responseCode = "404", description = "Tom respons når angitt prosesstask-id ikke finnes"),
        @ApiResponse(responseCode = "400", description = "Feil input")
    })
    public Response finnFeiletProsessTask(@NotNull @Parameter(description = "Prosesstask-id for feilet prosesstask") @Valid @BeanParam ProsessTaskIdDto prosessTaskIdDto) {
        sjekkAtSaksbehandlerHarRollenDrift();
        var resultat = prosessTaskApplikasjonTjeneste.finnFeiletProsessTask(prosessTaskIdDto.getProsessTaskId());
        if (resultat.isPresent()) {
            return Response.ok(resultat.get()).build();
        }
        return Response.status(HttpURLConnection.HTTP_NOT_FOUND).build();
    }

    @POST
    @Path("/setferdig/{prosessTaskId}/{prosessTaskStatus}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter feilet prosesstask med angitt prosesstask-id til FERDIG (kjøres ikke)", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Angitt prosesstask-id satt til status FERDIG"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    public Response setFeiletProsessTaskFerdig(@Parameter(description = "Prosesstask-id for feilet prosesstask") @NotNull @Valid @BeanParam ProsessTaskSetFerdigInputDto prosessTaskIdDto) {
        sjekkAtSaksbehandlerHarRollenDrift();
        prosessTaskApplikasjonTjeneste.setProsessTaskFerdig(prosessTaskIdDto.getProsessTaskId(), mapIkkeFerdigTaskStatus(prosessTaskIdDto.getNaaVaaerendeStatus()));
        return Response.ok().build();
    }

    private ProsessTaskStatus mapIkkeFerdigTaskStatus(IkkeFerdigProsessTaskStatusEnum naavarendeTaskStatus) {
        return switch (naavarendeTaskStatus) {
            case FEILET -> ProsessTaskStatus.FEILET;
            case VENTER_SVAR -> ProsessTaskStatus.VENTER_SVAR;
            case KLAR -> ProsessTaskStatus.KLAR;
            case SUSPENDERT -> ProsessTaskStatus.SUSPENDERT;
            case VETO -> ProsessTaskStatus.VETO;
        };
    }

    static void sjekkAtSaksbehandlerHarRollenDrift() {
        var kontekst = KontekstHolder.getKontekst();
        if (erSaksbehandler(kontekst) && saksbehandlerHarRollenDrift(kontekst)) {
            return;
        }
        throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_MANGLER_DRIFT_ROLLE);
    }

    private static boolean erSaksbehandler(Kontekst kontekst) {
        if (kontekst == null) {
            return false;
        }
        return IdentType.InternBruker.equals(kontekst.getIdentType());
    }

    private static boolean saksbehandlerHarRollenDrift(Kontekst kontekst) {
        if (kontekst == null) {
            return false;
        }
        return kontekst instanceof RequestKontekst requestKontekst && requestKontekst.harGruppe(AnsattGruppe.DRIFT);
    }

}
