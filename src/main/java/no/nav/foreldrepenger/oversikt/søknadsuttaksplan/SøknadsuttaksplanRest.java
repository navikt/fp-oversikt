package no.nav.foreldrepenger.oversikt.søknadsuttaksplan;

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

@Path("/soknadsuttaksplan")
@ApplicationScoped
@Transactional
public class SøknadsuttaksplanRest {

    private SøknadsuttaksplanTjeneste søknadsuttaksplanTjeneste;
    private TilgangKontrollTjeneste tilgangKontroll;

    @Inject
    public SøknadsuttaksplanRest(SøknadsuttaksplanTjeneste søknadsuttaksplanTjeneste, TilgangKontrollTjeneste tilgangKontroll) {
        this.søknadsuttaksplanTjeneste = søknadsuttaksplanTjeneste;
        this.tilgangKontroll = tilgangKontroll;
    }

    SøknadsuttaksplanRest() {
        // CDI
    }

    @POST
    public void lagre(@Valid @NotNull SøknadsuttaksplanRequest request) {
        tilgangKontroll.sjekkAtKallErFraSystem();
        var saksnummer = new no.nav.foreldrepenger.oversikt.domene.Saksnummer(request.saksnummer().value());
        var perioder = SøknadsuttaksplanMapper.tilPerioder(request.perioder());
        søknadsuttaksplanTjeneste.lagre(saksnummer, new Søknadsuttaksplan(request.mottattTidspunkt().toInstant(), request.rolle(), perioder), request.søknadType());
    }

    public record SøknadsuttaksplanRequest(@Valid @NotNull Saksnummer saksnummer,
                               @NotNull OffsetDateTime mottattTidspunkt,
                               @Valid @NotNull Søknadsuttaksplan.Rolle rolle,
                               @Valid @NotNull SøknadType søknadType,
                               @NotNull @Size(min = 1) List<@Valid @NotNull UttakPeriodeDto> perioder) {
        @Override
        public String toString() {
            return "SøknadsuttaksplanRequest[skjermet]";
        }
    }
}
