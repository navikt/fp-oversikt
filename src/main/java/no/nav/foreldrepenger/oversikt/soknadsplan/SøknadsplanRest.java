package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto.UttakPeriodeDto;

@Path("/soknadsplan")
@ApplicationScoped
@Transactional
public class SøknadsplanRest {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadsplanRest.class);

    private SøknadsplanTjeneste søknadsplanTjeneste;
    private TilgangKontrollTjeneste tilgangKontroll;
    private boolean lagringAktivert;

    @Inject
    public SøknadsplanRest(SøknadsplanTjeneste søknadsplanTjeneste, TilgangKontrollTjeneste tilgangKontroll) {
        // TODO: Fjern når søknadsplan skal tas i bruk i prod
        this(søknadsplanTjeneste, tilgangKontroll, !Environment.current().isProd());
    }

    SøknadsplanRest(SøknadsplanTjeneste søknadsplanTjeneste, TilgangKontrollTjeneste tilgangKontroll, boolean lagringAktivert) {
        this.søknadsplanTjeneste = søknadsplanTjeneste;
        this.tilgangKontroll = tilgangKontroll;
        this.lagringAktivert = lagringAktivert;
    }

    SøknadsplanRest() {
        // CDI
    }

    @POST
    public void lagre(@Valid @NotNull SøknadsplanRequest request) {
        tilgangKontroll.sjekkAtKallErFraSystem();
        var saksnummer = new no.nav.foreldrepenger.oversikt.domene.Saksnummer(request.saksnummer().value());
        var perioder = SøknadsplanMapper.tilPerioder(request.perioder());
        if (!lagringAktivert) {
            // Tar imot og mapper kallet i prod for å verifisere integrasjonen med fp-soknad, men lagrer ikke
            LOG.info("Lagring av søknadsplan er deaktivert i dette miljøet");
            return;
        }
        søknadsplanTjeneste.lagre(saksnummer, new Søknadsplan(request.mottattTidspunkt().toInstant(), request.rolle(), perioder));
    }

    public record SøknadsplanRequest(@Valid @NotNull Saksnummer saksnummer,
                               @NotNull OffsetDateTime mottattTidspunkt,
                                @Valid @NotNull Søknadsplan.Rolle rolle,
                               @NotNull @Size(min = 1) List<@Valid @NotNull UttakPeriodeDto> perioder) {
        @Override
        public String toString() {
            return "SøknadsplanRequest[skjermet]";
        }
    }
}
