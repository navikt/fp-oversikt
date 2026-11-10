package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakPeriodeDto;

@Path("/soknadsplan")
@ApplicationScoped
@Transactional
public class SøknadsplanRest {

    // Samme grense som fpsoknad validerer søknaden mot (UttaksperioderValidering.MAKS_ANTALL_PERIODER_TOTALT)
    static final int MAKS_ANTALL_PERIODER = 400;

    private SøknadsplanTjeneste søknadsplanTjeneste;
    private TilgangKontrollTjeneste tilgangKontroll;

    @Inject
    public SøknadsplanRest(SøknadsplanTjeneste søknadsplanTjeneste, TilgangKontrollTjeneste tilgangKontroll) {
        this.søknadsplanTjeneste = søknadsplanTjeneste;
        this.tilgangKontroll = tilgangKontroll;
    }

    SøknadsplanRest() {
        // CDI
    }

    /**
     * Alle systemklienter i accessPolicy.inbound får skrive søknadsplan. Saksnummer kontrolleres ikke mot bruker,
     * så kalleren må hente saksnummeret fra FPSAK og ikke fra sluttbruker. fpsoknad gjør dette i DestinasjonsRuter.
     */
    @POST
    public void lagre(@Valid @NotNull SøknadsplanRequest request) {
        tilgangKontroll.sjekkAtKallErFraSystem();
        var saksnummer = new no.nav.foreldrepenger.oversikt.domene.Saksnummer(request.saksnummer().value());
        var perioder = SøknadsplanMapper.tilPerioder(request.perioder());
        søknadsplanTjeneste.lagre(saksnummer, new Søknadsplan(request.mottattTidspunkt(), request.rolle(), perioder));
    }

    public record SøknadsplanRequest(@Valid @NotNull Saksnummer saksnummer,
                                     @NotNull LocalDateTime mottattTidspunkt,
                                     @Valid @NotNull Søknadsplan.Rolle rolle,
                                     @NotNull @Size(min = 1, max = MAKS_ANTALL_PERIODER) List<@Valid @NotNull UttakPeriodeDto> perioder) {
        @Override
        public String toString() {
            return "SøknadsplanRequest[skjermet]";
        }
    }
}
