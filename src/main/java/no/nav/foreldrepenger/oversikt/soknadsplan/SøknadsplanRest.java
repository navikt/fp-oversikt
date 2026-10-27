package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.time.OffsetDateTime;
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

    @POST
    public void lagre(@Valid @NotNull SøknadsplanRequest request) {
        tilgangKontroll.sjekkAtKallErFraSystem();
        var saksnummer = new no.nav.foreldrepenger.oversikt.domene.Saksnummer(request.saksnummer().value());
        var perioder = SøknadsplanMapper.tilPerioder(request.perioder());
        søknadsplanTjeneste.lagre(saksnummer, new Søknadsplan(request.mottattTidspunkt().toInstant(), request.rolle(), perioder), request.søknadType());
    }

    public record SøknadsplanRequest(@Valid @NotNull Saksnummer saksnummer,
                               @NotNull OffsetDateTime mottattTidspunkt,
                                @Valid @NotNull Søknadsplan.Rolle rolle,
                               @Valid @NotNull SøknadType søknadType,
                               @NotNull @Size(min = 1) List<@Valid @NotNull UttakPeriodeDto> perioder) {
        @Override
        public String toString() {
            return "SøknadsplanRequest[skjermet]";
        }
    }
}
