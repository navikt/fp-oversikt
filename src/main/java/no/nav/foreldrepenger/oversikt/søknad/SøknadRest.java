package no.nav.foreldrepenger.oversikt.søknad;

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

@Path("/soknad")
@ApplicationScoped
@Transactional
public class SøknadRest {

    private SøknadTjeneste søknadTjeneste;
    private TilgangKontrollTjeneste tilgangKontroll;

    @Inject
    public SøknadRest(SøknadTjeneste søknadTjeneste, TilgangKontrollTjeneste tilgangKontroll) {
        this.søknadTjeneste = søknadTjeneste;
        this.tilgangKontroll = tilgangKontroll;
    }

    SøknadRest() {
        // CDI
    }

    @POST
    public void lagre(@Valid @NotNull SøknadRequest request) {
        tilgangKontroll.sjekkAtKallErFraSystem();
        var saksnummer = new no.nav.foreldrepenger.oversikt.domene.Saksnummer(request.saksnummer().value());
        var perioder = SøknadMapper.tilPerioder(request.perioder());
        søknadTjeneste.lagre(saksnummer, new Søknad(request.mottattTidspunkt().toInstant(), request.rolle(), perioder), request.søknadType());
    }

    public record SøknadRequest(@Valid @NotNull Saksnummer saksnummer,
                               @NotNull OffsetDateTime mottattTidspunkt,
                               @Valid @NotNull Søknad.Rolle rolle,
                               @Valid @NotNull SøknadType søknadType,
                               @NotNull @Size(min = 1) List<@Valid @NotNull UttakPeriodeDto> perioder) {
        @Override
        public String toString() {
            return "SøknadRequest[skjermet]";
        }
    }
}
